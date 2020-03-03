dbs {

    atiIntelliscan {

        excludedColumns = ['password', 'pwd', 'foo']

        tables {
            account_setting
            asset
            asset_attribute
            asset_group
            asset_tag
            device
            image_event
            image_metadata
            image_request
            sensor_info
            image_credit_limit
            image_credit_limit_history
        }
    }

}
