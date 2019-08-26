@Grab('com.amazonaws:aws-java-sdk:1.11.511')
@Grab('net.snowflake:snowflake-jdbc:3.6.27')
@Grab('mysql:mysql-connector-java:5.1.47')
@Grab('org.codehaus.gpars:gpars:1.2.1')
@Grab('com.google.guava:guava:27.0.1-jre')
@GrabConfig(systemClassLoader=true)

import groovyx.gpars.*
import groovy.sql.*
import groovy.json.JsonBuilder

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.GZIPOutputStream

import com.amazonaws.services.s3.AmazonS3Client
import com.google.common.base.CaseFormat


//def idealChunkingRecordCount = 100000

// database_config extract_config scratch_dir daysBack

def config = new ConfigSlurper().parse(new File(args[0]).toURL())
def extractConfig = new ConfigSlurper().parse(new File(args[1]).toURL())
def scratchDir = new File(args[2])
def daysBack = args[3] as Integer // -1 is full extract
def idealChunkingRecordCount = args[4]  as Integer

def globallyExcludedColumns = ['password','pwd'] as Set
globallyExcludedColumns = extractConfig.db.excludedColumns ? extractConfig.db.excludedColumns + globallyExcludedColumns as Set : globallyExcludedColumns

def concurrentExtracts = extractConfig.concurrentExtracts ?: 1

//def daysBack = extractConfig.daysBack ?: 5

def sfUrl       = "${config.snowflakeDb.url}&warehouse=${config.snowflakeDb.warehouse}&db=${config.snowflakeDb.db}"
def sfUsername  = config.snowflakeDb.username
def sfPassword  = config.snowflakeDb.password
def sfDb        = config.snowflakeDb.db

println "   excluding columns $globallyExcludedColumns for all tables..."
println "   starting extract process. tables will be extracted ${concurrentExtracts} at a time..."

extractConfig.dbs.each { db ->

    def dbConfigKey = "${db.key}Db"

    preProcessing(sfUrl, sfUsername, sfPassword, sfDb, db.value?.preSql)

    db.value.tables.each { t ->

        def sqlConn = Sql.newInstance(config."$dbConfigKey".url, config."$dbConfigKey".username, config."$dbConfigKey".password, 'com.mysql.jdbc.Driver')
        def database = db.key
        def table = t.key

        try {

            // get table schema (used to generate view in snowflake)
            def desc = sqlConn.rows("describe ${table}".toString())
            def watermark = desc.any { it.Field == 'last_updated'} ? 'last_updated' : t.value?.watermark    // watermark or not
            def customId = t.value?.customId ?: "id"        // id there is no id in table, you can define one (create a composite field for example)

            def fields = filterFields(desc, t.value.includedColumns, t.value?.excludedColumns ? t.value?.excludedColumns + globallyExcludedColumns as Set : globallyExcludedColumns)

            // def includedColumns = t.value.includedColumns ? t.value.includedColumns.join(', ') : '*'
            // def excludedColumns = t.value?.excludedColumns ? t.value?.excludedColumns + globallyExcludedColumns as Set : globallyExcludedColumns

            println "-------------------- starting on '${database}.${table}' --------------------\t${getFreeMemoryGb()} GBs of ${getMaxMemoryGb()} GBs available"

            // generate extract file
            def files
            def extractTs
            retry(3) {
                extractTs = Instant.now().toEpochMilli()
                if (watermark && daysBack >= 1) {
                    files = []
                    files = extractToFiles(scratchDir, sqlConn, database, table, fields, daysBack, watermark, customId, extractTs, idealChunkingRecordCount, 32)
                } else {
                    files = []
                    files = extractToFiles(scratchDir, sqlConn, database, table, fields, null, null, customId, extractTs, idealChunkingRecordCount, 32)
                }
            }

            File extractedIdsFile
            retry(3){
                extractedIdsFile = extractIds(scratchDir, sqlConn, database, table, customId)
            }


            // upload file to s3
            def targetBucket = config.target.aws.s3.bucket
            def targetPrefix = config.target.aws.s3.prefix
            def concurrentUploads = 8
            GParsPool.withPool(concurrentUploads) {
                files?.eachParallel { file ->
                    retry(10) {
                        uploadToS3(database, table, file, targetBucket, targetPrefix)
                    }
                }
            }

            retry(3){
                uploadToS3(database, table, extractedIdsFile, targetBucket, targetPrefix)
            }

            // ingest file into data warehouse
            def sfDatabase = config.snowflakeDb.db
            def sfSchema = config.snowflakeDb.schema
            def sfTable = table.toUpperCase()
            def sfStage = config.snowflakeDb.stage
            retry(3) {
                ingestIntoSnowflake(sfUrl, sfUsername, sfPassword, sfStage, sfDatabase, sfSchema, sfTable, database, table, fields, files, extractedIdsFile, extractTs)
            }

            // clean up files
           files?.each { file ->
               file.delete()
           }
           extractedIdsFile.delete()


            println "-------------------- finished '${database}.${table}' -----------------------\t${getFreeMemoryGb()} GBs of ${getMaxMemoryGb()} GBs available\n"

        } catch(e) {
            println(e)
            println("Failed to extract '${database}.${table}'. Moving on to next table")
        }

    }

    postProcessing(sfUrl, sfUsername, sfPassword, sfDb, db.value?.postSql)
}

File extractIds(scratchDir, sqlConn, database, table, customId){

    def file = new File(scratchDir, "${table}_ids.csv.gz".toString())
    def etl_id = customId ?: "id"

    def selectIds = """
            select $etl_id etl_id from ${table} where $etl_id is not null;
    """
    println(selectIds)
    sqlConn?.execute("set transaction isolation level read uncommitted".toString())

    def rowCount = 0
    def gzip = new GZIPOutputStream(new FileOutputStream(file))
    new BufferedWriter(new OutputStreamWriter(gzip, "UTF-8")).withWriter { writer ->
        sqlConn?.rows(selectIds.toString())?.each { row ->
            writer.writeLine("${row['etl_id']}")
            rowCount++
            if (rowCount % 10000 == 0) {
                writer.flush()
            }
        }
        writer.flush()
        writer.close()
    }
    gzip.close()

    return file
}

/**
 * Extract table contents into gzipped files in a json format.
 */
File[] extractToFiles(File scratchDir, sqlConn, database, table, fields, daysBack, watermark, customId, extractTs, idealChunkingRecordCount, int numberOfThreads = 8) {

//    def etl_id = table == "app_system_app_brand" ? "((app_system_brands_id * 10000000) + (app_brand_id))" : "id"

    def etl_id = customId ?: "id"
    def etl_wm = daysBack >=1 ? "UNIX_TIMESTAMP(${watermark})" : "0"


    sqlConn?.execute("set transaction isolation level read uncommitted".toString())
    def baseFileName = "${table}_"
    def baseFileExt = ".json.gz"
    def files = []

    // no point chunking out if number of records is too small
    def sqlTotalTableCount = """SELECT count(*) as c FROM ${table} WHERE $etl_id is not null """.toString()
    if (daysBack){
        sqlTotalTableCount += """ AND $etl_wm > UNIX_TIMESTAMP(DATE_ADD(CURRENT_DATE(), INTERVAL -${daysBack} DAY))"""
    }

    println("Counting records from '$table'... $sqlTotalTableCount")
    def totalTableCount = sqlConn.firstRow(sqlTotalTableCount).c

    if (totalTableCount<1) {
        println("Nothing to extract. Moving along.")
    } else {

        // figure out how many files needed for chunking
        int partCount = getChunkCount(totalTableCount, idealChunkingRecordCount)


        // started chunked extraction
        println("Starting to process '$totalTableCount' records into '${partCount}' files...")
        AtomicInteger totalExtracted = new AtomicInteger(0)
        GParsPool.withPool(numberOfThreads) {

            (0..partCount - 1).eachParallel { part ->

                def fileName = baseFileName + "${part}".padLeft(6, '0') + baseFileExt
                def file = new File(scratchDir, fileName.toString())

                def columns = fields.collect { it.Field }.join(',')
                def sqlQuery = """
                    SELECT
                        ${columns},
                        $etl_id etl_id, $etl_wm etl_wm
                    FROM ${table} WHERE $etl_id is not null
                """

                if (partCount > 1) {
                    sqlQuery += """ AND $etl_id % ${partCount} = ${part} """
                }
                if (daysBack >= 1) {
                    sqlQuery += """ AND $etl_wm > UNIX_TIMESTAMP(DATE_ADD(CURRENT_DATE(), INTERVAL -${
                        daysBack
                    } DAY)) """
                }

                def rowCount = 0
                // write out to gz file
                def gzip = new GZIPOutputStream(new FileOutputStream(file))
                new BufferedWriter(new OutputStreamWriter(gzip, "UTF-8")).withWriter { writer ->

                    println """\t${part}) Extracting results from '$table': $sqlQuery"""

                    sqlConn?.rows(sqlQuery.toString())?.each { row ->
                        row.put("extractTs", extractTs)
                        writer.writeLine(new JsonBuilder(row).toString())
                        rowCount++
                        if (rowCount % 1000 == 0) {
                            writer.flush()
                        }   // write out to file in batches
                    }
                    writer.flush()
                    writer.close()
                }
                gzip.close()

                if (rowCount > 0) {
                    files << file
                }
                def currentExtracted = totalExtracted.addAndGet(rowCount)
                println "\t${part}) Processed ${rowCount} records from '${database}.${table}'. ${((currentExtracted / totalTableCount) * 100) as int}% of total."


            }
        }
        println("\tFinished. Processed ${totalExtracted.get()} total records from '${database}.${table}'")
    }
    return files

}


def getChunkCount(totalTableCount, idealChunkingRecordCount){
    int partCount
    if (totalTableCount < idealChunkingRecordCount){
        return 1
    } else {
        int potentialChunkCount = totalTableCount / idealChunkingRecordCount + 1
        if (potentialChunkCount>512){
            // SF limit is 1000 file list in copy command, we'll keep it at 512 just to be safe
            return 512
        } else {
            // less than 512, should be good
            return potentialChunkCount
        }
    }
    return partCount
}


def filterFields(fields, included, excluded){

    fields.each { sfMappings(it) }
    if (included){
        return fields.findAll { it.Key || it.Field in included }
    } else if (excluded){
        return fields.findAll { it.Key || !(it.Field in excluded) }
    } else {
        return fields
    }
}


def sfMappings(field){


    if (field.Type.toLowerCase().startsWith('bit')){
        field.sfType = 'BOOLEAN'
        field.sfExtract = "JSON:${field.Field}::BOOLEAN as ${field.Field}"
    } else if (field.Type.toLowerCase().startsWith('bigint')){
        field.sfType = 'NUMBER'
        field.sfExtract = "JSON:${field.Field}::NUMBER as ${field.Field}"
    } else if (field.Type.toLowerCase().startsWith('int')){
        field.sfType = 'NUMBER'
        field.sfExtract = "JSON:${field.Field}::NUMBER as ${field.Field}"
    } else if (field.Type.toLowerCase().startsWith('double')){
        field.sfType = 'DOUBLE'
        field.sfExtract = "JSON:${field.Field}::DOUBLE as ${field.Field}"
    } else if (field.Type.toLowerCase().startsWith('decimal')){
        field.sfType = 'DECIMAL'
        field.sfExtract = "JSON:${field.Field}::DECIMAL as ${field.Field}"
    } else if (field.Type.toLowerCase().startsWith('float')){
        field.sfType = 'FLOAT'
        field.sfExtract = "JSON:${field.Field}::FLOAT as ${field.Field}"
    } else if (field.Type.toLowerCase().startsWith('datetime')){
        field.sfType = 'DATETIME'
        field.sfExtract = "TO_TIMESTAMP(AS_VARCHAR(JSON:${field.Field}), 'YYYY-MM-DDTHH24:MI:SSTZHTZM') as ${field.Field}"
    } else if (field.Type.toLowerCase().startsWith('timestamp')){
        field.sfType = 'DATETIME'
        field.sfExtract = "TO_TIMESTAMP(AS_VARCHAR(JSON:${field.Field}), 'YYYY-MM-DDTHH24:MI:SSTZHTZM') as ${field.Field}"
    } else if (field.Type.toLowerCase().startsWith('longtext')){
        field.sfType = 'STRING'
        field.sfExtract = "JSON:${field.Field}::STRING as ${field.Field}"
    } else if (field.Type.toLowerCase().startsWith('varchar')){
        field.sfType = 'STRING'
        field.sfExtract = "JSON:${field.Field}::STRING as ${field.Field}"
    } else if (field.Type.toLowerCase().startsWith('nvarchar')){
        field.sfType = 'STRING'
        field.sfExtract = "JSON:${field.Field}::STRING as ${field.Field}"
    } else {
        println "unmapped type ${field.Type}"
        field.sfType = 'STRING'
        field.sfExtract = "JSON:${field.Field}::STRING as ${field.Field}"
    }

}


/**
 * Upload extract file to s3 bucket with given prefix
 */
def uploadToS3(database, table, file, targetBucket, targetPrefix) {

    // copy extract to S3
    def s3Location = "${targetPrefix}/${database}/${table}/${file.name}"
    println """Uploading ${file.name} to "s3://$targetBucket/$s3Location"..."""
    def s3Client = new AmazonS3Client()
    s3Client.putObject(targetBucket, s3Location, file)
    println """Uploaded ${file.name} to "s3://$targetBucket/$s3Location"."""

}

def selectAndCastCriteria(fields){
    fields.collect {
        if (it.SfType.equals('DATETIME')) {
            ""
        } else {
            "JSON:${it.Field}::${it.SfType} AS ${it.Field}"
        }
    }.join(', ')
}

/**
 *  Ingest extract file from stage location (s3 through snowflake stage) into snowflake database
 */
def ingestIntoSnowflake(sfUrl, sfUsername, sfPassword, sfStage, sfDatabase, sfSchema, sfTable, sourceDatabase, sourceTable, fields, files, extractedIdsFile, extractTs){

    def sfS3Stage = "\"${sfDatabase}\".\"${sfSchema}\".\"${sfStage}\""


    def schema          = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, sourceDatabase)
    def tempIngestTable = "\"${schema}\".\"ETL_TEMP_${sfTable}\""
    def stagingTable    = "\"${schema}\".\"ETL_JSON_${sfTable}\""
    def table           = "\"${schema}\".\"${sfTable}\""
    def idTable         = "\"${schema}\".\"ETL_IDS_${sfTable}\""

    def statsTable      = "\"${schema}\".ETL_STATS"

    def CTAS_CRITERIA = fields.collect { it.sfExtract }.join(', ') + ", ETL_ID, ETL_WM "

    // backwards compatibility
    def legacySchema    = "\"TEMP_${sourceDatabase}\""
    def legacyView      = "\"TEMP_${sourceDatabase}\".\"${sfTable}\""

    def sfIngestSql = [

            """
                USE DATABASE $sfDatabase;
            """,

            """
                CREATE SCHEMA IF NOT EXISTS $schema;
            """,

            """
                CREATE OR REPLACE TABLE $idTable (ETL_ID STRING);
            """,

            """
                COPY INTO $idTable
                FROM @${sfS3Stage}/${sourceDatabase}/${sourceTable}/
                FILES = ('${extractedIdsFile.name}')
                PURGE = TRUE;
            """,
    ]

    if (files){

        def fileNames = files.collect { "'${it.name}'" }.join(',')

        sfIngestSql += [

            """
                CREATE OR REPLACE TEMP TABLE $tempIngestTable (JSON VARIANT);
            """,

            """
                COPY INTO $tempIngestTable
                FROM @${sfS3Stage}/${sourceDatabase}/${sourceTable}/
                FILES = (${fileNames})
                FILE_FORMAT = (TYPE = 'JSON' STRIP_NULL_VALUES = TRUE)
                PURGE = TRUE;
            """,

            """
                CREATE TABLE IF NOT EXISTS $stagingTable
                (ETL_ID STRING, ETL_WM NUMBER, JSON VARIANT, LAST_SYNC_TS NUMBER);
            """,


            """
                MERGE INTO $stagingTable tgt
                USING $tempIngestTable stg ON
                    stg."JSON":etl_id = tgt.ETL_ID
                    and stg."JSON":etl_wm = tgt.ETL_WM
                WHEN NOT MATCHED THEN INSERT
                    (ETL_ID, ETL_WM, JSON, LAST_SYNC_TS) VALUES
                    (stg."JSON":etl_id, stg."JSON":etl_wm, stg."JSON", stg."JSON":extractTs);
            """,

            """
                DROP TABLE IF EXISTS $tempIngestTable;
            """,

        ]

    }


    sfIngestSql += [
            """
                CREATE OR REPLACE TABLE $table
                COPY GRANTS
                AS SELECT $CTAS_CRITERIA
                FROM (
                    SELECT
                        ETL_ID, ETL_WM, JSON, LAST_SYNC_TS,
                         ROW_NUMBER() OVER (PARTITION BY ETL_ID ORDER BY ETL_WM DESC) AS row_num
                    FROM $stagingTable
                    WHERE ETL_ID IN (SELECT ETL_ID FROM $idTable)
                ) WHERE row_num = 1;

            """,

            """
                GRANT SELECT ON VIEW $table
                TO ROLE PROCON_READ_ONLY;
            """,

            """
                CREATE TABLE $statsTable IF NOT EXISTS (TABLE_NAME STRING, RECORD_COUNT NUMBER, SRC_TABLE_COUNT NUMBER, EXTRACT_START NUMBER, EXTRACT_DURATION_MS NUMBER);
            """,

            """
                INSERT INTO $statsTable
                WITH
                    TABLE_COUNT AS (SELECT COUNT(*) C FROM $table),
                    ID_TABLE_COUNT AS (SELECT COUNT(*) C FROM $idTable)
                SELECT
                    '$sfTable' TABLE_NAME,
                    TABLE_COUNT.C RECORD_COUNT,
                    ID_TABLE_COUNT.C SRC_TABLE_COUNT,
                    ${extractTs} EXTRACT_START,
                    ${Instant.now().toEpochMilli() - extractTs} EXTRACT_DURATION_MS
                FROM TABLE_COUNT, ID_TABLE_COUNT;
            """
    ]


//    if (sourceDatabase.toLowerCase().startsWith("proconcore") || sourceDatabase.toLowerCase().startsWith("shopbert") || sourceDatabase.toLowerCase().startsWith("automotiveServices")){
//        def targetEntity = "\"$sfDatabase\".\"NSPIRE\".\"$sfTable\""
//        // sfIngestSql << """ DROP TABLE IF EXISTS $targetEntity"""
//        sfIngestSql << """ CREATE OR REPLACE VIEW $targetEntity COPY GRANTS AS SELECT JSON, LAST_SYNC_TS FROM $sfDbSchemaTableRaw """
//    }


    try {
        // run sql instructions
        Sql.newInstance(sfUrl, sfUsername, sfPassword, 'com.snowflake.client.jdbc.SnowflakeDriver').with { sfsql ->
            sfIngestSql.each {
                println it
                sfsql.execute(it.toString())
            }
        }
    } catch(e){
        println "failed. skipping table '$table'."
    }

}


def retry(int times = 5, Closure body) {
    int retries = 0
    def exceptions = []
    while(retries++ < times) {
        try {
            return body.call()
        } catch(e) {
            e.printStackTrace()
            exceptions << e
        }
    }
    println("Failed after $times retries: ${exceptions}")
    exceptions[-1].printStackTrace()
    throw exceptions[-1]
}

def postProcessing(sfUrl, sfUsername, sfPassword, sfDb, statements){
    if (statements) {
        statements.add(0, "USE DATABASE $sfDb;")
        println("Staring post processing...")
        Sql.newInstance(sfUrl, sfUsername, sfPassword, 'com.snowflake.client.jdbc.SnowflakeDriver').with { sfsql ->
            statements.each { stmt ->
                println "\t$stmt"
                sfsql.execute(stmt.toString())
            }
        }
        println("Finished post processing.")
    } else {
        println("No post processing to be done.")
    }
}


def preProcessing(sfUrl, sfUsername, sfPassword, sfDb, statements){
    if (statements) {
        statements.add(0, "USE DATABASE $sfDb;")
        println("Staring pre processing...")
        Sql.newInstance(sfUrl, sfUsername, sfPassword, 'com.snowflake.client.jdbc.SnowflakeDriver').with { sfsql ->

            statements.each { stmt ->
                println "\t$stmt"
                sfsql.execute(stmt.toString())
            }
        }
        println("Finished pre processing.")
    } else {
        println("No pre processing to be done.")
    }
}

//def getTotalMemoryGb() {
//    return ((Runtime.getRuntime().totalMemory()) / 1024 / 1024 / 1024)
//}

def getMaxMemoryGb(){
    return new Double((Runtime.getRuntime().maxMemory()) / 1024 / 1024 / 1024).round(2)
}

def getFreeMemoryGb(){
    return new Double((Runtime.getRuntime().freeMemory()) / 1024 / 1024 / 1024).round(2)
}

false

