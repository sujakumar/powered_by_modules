dbs {

    fcms {

        excludedColumns = ['password', 'pwd', 'foo']

        tables {
            fcms_asset
			fcms_transaction
			fuel_card_asset_association
			location
        }
		postSql = [
			"CALL FCMS.POST_PROCESS();"
		]

    }

}

