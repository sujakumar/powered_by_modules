concurrentExtracts = 1
daysBack = -1

dbs {

	proconCore {

		// these columns will always be excluded from extract files
		excludedColumns = ['password', 'pwd', 'foo']

		// tables to extract
		tables {
			operator
			account
			account_link
			account_user
			account_preference
			account_dealer
			app_brand
			app_system
			app_system_brand
			app_system_app_brand {
				customId = "((app_system_brands_id * 10000000) + (app_brand_id))"
			}
			app_brand_map
			asset
			asset_group
			asset_operator
			device
			device_link
			device_network
			user
			user_preference
			invitation
			geofence
			account_user_universe {
				customId = "((account_user_security_universes_id * 100000000) + (universe_id))"
			}
			universe
			landmark
			landmark_group

		}
	}

}
