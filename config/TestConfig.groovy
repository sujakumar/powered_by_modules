concurrentExtracts = 1
daysBack = -1

dbs {

    proconCore {

        // these columns will always be excluded from extract files
        excludedColumns = ['password', 'pwd', 'foo']

        // tables to extract
        tables {
		operator
		app_system_app_brand {
			customId = "((app_system_brands_id * 10000000) + (app_brand_id))"
		}	
//		asset
//		device
/*
            device_command
            device_command_type
            device_event_type
            device_link
            device_network
            device_network_provider
            device_type
            device_model
            device_share
*/
//	landmark
//asset_operator
//		account
//		asset_group
//		app_system
//		app_brand
//		app_system_brand
        }
        postSql = [
                "USE SCHEMA FAI;"
		,"CALL INIT_BUSINESS_OBJECTS();"
        ]
    }

}




