dbs {

    dealerTrackAdapter {

        // these columns will always be excluded from extract files
        excludedColumns = ['password', 'pwd', 'foo']

        // tables to extract
        tables {

            deal_status_mapping {
                customId = '(CONCAT(dealership, "||", deal_status, "||", status_code))'
            }
            dt_deal
            dt_deal_fees {
                customId = '(CONCAT(deal, "||", amo_fee_amount, "||", cost, "||", record_type))'
            }
            product_code_mapping {
                customId = '(CONCAT(dealership, "||", external_code, "||", product_code))'
            }

        }

    }

}




