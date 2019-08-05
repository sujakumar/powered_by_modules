dbs {

    shopbert {

        excludedColumns = ['password', 'pwd', 'foo']

        tables {
            shop_bert_customer
            shop_bert_line_item
            shop_bert_line_item_attribute
            shop_bert_web_order
        }
		postSql = [
			"CALL SHOPBERT.POST_PROCESS();"
		]

    }

}

