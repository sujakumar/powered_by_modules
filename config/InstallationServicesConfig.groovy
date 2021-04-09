dbs {

    installationServices{

        // these columns will always be excluded from extract files
        excludedColumns = ['password', 'pwd', 'foo']

        // tables to extract
        tables {
            installation

        }

    }

}