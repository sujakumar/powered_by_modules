dbs {

    maintenanceService {

        // these columns will always be excluded from extract files
        excludedColumns = ['password', 'pwd', 'foo']

        // tables to extract
        tables {

            asset
            maintenance_plan

        }
    }

}
