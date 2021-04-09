dbs {

    enterpriseIntegration {

        // these columns will always be excluded from extract files
        excludedColumns = ['password', 'pwd', 'foo']

        // tables to extract
        tables {
           eis_order
           eis_order_item
           fulfillment_record
        }

    }

}