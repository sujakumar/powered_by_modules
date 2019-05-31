dbs {

	cdkAdapter {

		// these columns will always be excluded from extract files
		excludedColumns = ['password', 'pwd', 'foo']

		// tables to extract
		tables {

			cdk_deal
			dealership
			extraction_log

		}
		postSql = [
				"CALL CDK_ADAPTER.POST_PROCESS();"
		]
	}

}
