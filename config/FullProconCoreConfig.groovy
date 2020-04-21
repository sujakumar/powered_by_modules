
dbs {

    proconCore {

        // these columns will always be excluded from extract files
        excludedColumns = ['password', 'pwd', 'foo']

        // tables to extract
        tables {
            app_feature_set
            data_service_map
            device_battery_type
            asset_attribute
            asset_type
            asset_group_type
            device_command
            device_command_type
            device_event_type
            device_network_provider
            device_type
            device_model
            device_share
            lot
            attribute_type
            device_configuration {
                includedColumns = ['id', 'version', 'current_value', 'date_created', 'device_command_type_id', 'device_id', 'last_transaction_date', 'last_transaction_id', 'last_updated', 'request_date', 'requested_by', 'requested_value', 'retry', 'status', 'transaction_id', 'transmit_value']
            }

        }
    }

}
