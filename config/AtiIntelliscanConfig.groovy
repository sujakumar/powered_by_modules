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
        }
    }

}
