dbs {

    reynolds {

        excludedColumns = ['password', 'pwd', 'foo']

        tables {
            dealership
            reynolds_deal
            reynolds_message
            sale_aftermarkets{
                customId = '(CONCAT(reynolds_deal_id, "||", code))'
            }
            store
            product_code_mapping {
                customId = '(CONCAT(dealership, "||", external_code, "||", product_code))'
            }
        }
    }

}

