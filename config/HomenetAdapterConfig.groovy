dbs {
    homenetAdapter {
        excludedColumns = ['password', 'pwd', 'foo']
        tables {
            dealership
            extraction_log
            inventory_record
        }
    }
}
