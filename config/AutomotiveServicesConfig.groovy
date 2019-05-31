dbs {

    automotiveServices {

        excludedColumns = ['password', 'pwd', 'foo']

        tables {
            sale
            sale_item
	    sale_order
        }
	postSql = [
		"CALL AUTOMOTIVE_SERVICES.POST_PROCESS();"
	]

    }

}
