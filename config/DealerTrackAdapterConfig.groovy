dbs {

    dealerTrackAdapter {

        // these columns will always be excluded from extract files
        excludedColumns = ['password', 'pwd', 'foo']

        // tables to extract
        tables {
            dealership
            deal_status_mapping {
                customId = '(CONCAT(dealership, "||", deal_status, "||", status_code))'
            }
            dt_deal
            dt_deal_fees {
                customId = '(SHA1(CONCAT(COALESCE(deal, \'-\'), "||", COALESCE(amo_fee_amount, \'-\'), "||", COALESCE(cost, \'-\'), "||", COALESCE(record_type, \'-\'), "||", COALESCE(description, \'-\'))))'
            }
            product_code_mapping {
                customId = '(CONCAT(dealership, "||", external_code, "||", product_code))'
            }

        }

    }

}




