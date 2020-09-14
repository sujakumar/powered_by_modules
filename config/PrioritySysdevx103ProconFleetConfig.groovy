dbs {
    sysdevx103proconfleet {

        excludedColumns = ['password', 'pwd', 'foo', 'pin']

        tables {

            users {
                watermark = 'modified'
            }

            devices {
                watermark = 'modified'
            }

        }
    }
}
