dbs {

    dmsIntegration {

        // these columns will always be excluded from extract files
        excludedColumns = ['password', 'pwd', 'foo']

        // tables to extract
        tables {

            deal
            deal_item
            deal_validation_error
            processing_record

        }
	postSql = [
		"CALL DMS_INTEGRATION.POST_PROCESS();"
	]
    }

}
