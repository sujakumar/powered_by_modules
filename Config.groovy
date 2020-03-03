proconCoreDb {
    username    = '$extract_username'
    password    = '''$some_pwd'''
    url         = 'jdbc:mysql://amesnapshot01.i.spireon.com/proconCore_production?zeroDateTimeBehavior=convertToNull&connectTimeout=30000&socketTimeout=60000&autoReconnect=true&allowMultiQueries=true'
}

shopbertDb {
    username    = '$extract_username'
    password    = '''$some_pwd'''
    url         = 'jdbc:mysql://amesnapshot01.i.spireon.com/shopbert_production?zeroDateTimeBehavior=convertToNull&connectTimeout=30000&socketTimeout=60000&autoReconnect=true&allowMultiQueries=true'
}

automotiveServicesDb {
    username    = '$extract_username'
    password    = '''$some_pwd'''
    url         = 'jdbc:mysql://amesnapshot01.i.spireon.com/automotiveServices_production?zeroDateTimeBehavior=convertToNull&connectTimeout=30000&socketTimeout=60000&autoReconnect=true&allowMultiQueries=true'
}

cdkAdapterDb {
    username    = '$extract_username'
    password    = '''$some_pwd'''
    url         = 'jdbc:mysql://amesnapshot01.i.spireon.com/cdkAdapter_production?zeroDateTimeBehavior=convertToNull&connectTimeout=30000&socketTimeout=60000&autoReconnect=true&allowMultiQueries=true'
}

fcmsDb {
    username	= '$extract_username'
    password	= '''$some_pwd'''
    url		= 'jdbc:mysql://ame1-rds-fcms-cluster.cluster-ro-coiumbcrhgrg.us-east-1.rds.amazonaws.com/fcms?zeroDateTimeBehavior=convertToNull&connectTimeout=30000&socketTimeout=60000&autoReconnect=true&allowMultiQueries=true'
}

dmsIntegrationDb {
    username    = '$extract_username'
    password    = '''$some_pwd'''
    url         = 'jdbc:mysql://amesnapshot01.i.spireon.com/dmsIntegration_production?zeroDateTimeBehavior=convertToNull&connectTimeout=30000&socketTimeout=60000&autoReconnect=true&allowMultiQueries=true'
}


atiIntelliscan {
    username    = ''''''
    password    = ''''''
    url         = 'jdbc:mysql://??ati_intelliscan'
}


snowflakeDb {
    username    = "$sf_username"
    password    = '''$some_sf_pwd'''
    warehouse   = "LOAD_WH"
    db          = "$somedb"
    schema      = "NSPIRE"
    url         = "jdbc:snowflake://spireon.snowflakecomputing.com:443/?ssl=on&account=spireon"
    stage       = "S3_LAKESPIREON_PROD"
}



// where to store the extracted db files
target {
    aws {
        s3 {
            bucket = 'lakespireon'
            prefix = 'prod/nspire'
        }
    }
}
