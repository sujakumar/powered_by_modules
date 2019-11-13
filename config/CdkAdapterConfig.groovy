dbs {

	cdkAdapter {

		// these columns will always be excluded from extract files
		excludedColumns = ['password', 'pwd', 'foo']

		// tables to extract
		tables {

			cdk_deal
			dealership
			extraction_log
            rooftop
            product_code_mapping {
				customId = '(CONCAT(dealership, "||", external_code, "||", product_code))'
			}
            we_owe

		}

	}

}
