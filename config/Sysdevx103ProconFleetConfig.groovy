dbs {
    sysdevx103proconfleet {

        excludedColumns = ['password', 'pwd', 'foo', 'pin']
        tables {
            user_types
            brands
            plans
            plan_prices
            rma_master
            rma_fault
            rma_status
            order_serials
            order_details
            orders
            netsuite_special_classes
            service_types
            inventory_items
            device_status
            device_status_change_reasons
            contract_durations
            order_special_types
        }
    }
}
