dbs {

    shopbert {

        excludedColumns = ['password', 'pwd', 'foo']

        tables {
            shop_bert_customer
            shop_bert_line_item
            shop_bert_line_item_attribute
            shop_bert_web_order
            shop_bert_reference_type
        }
    }

}

