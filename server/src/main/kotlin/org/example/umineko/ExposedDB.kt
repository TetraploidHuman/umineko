package org.example.umineko

import org.example.umineko.DB.UserTable
import org.example.umineko.DB.demoTable
import org.example.umineko.DB.initTestDB
import org.example.umineko.DB.initUserDB
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

suspend fun initDB() {
    R2dbcDatabase.connect(
        "r2dbc:pool:h2:file:///C:/Users/LGVP2/IdeaProjects/Umineko/server/src/main/resources/demoTableData;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE?maxSize=4&initialSize=4"
    )

    suspendTransaction {
        SchemaUtils.create(demoTable)
        initTestDB()

        SchemaUtils.create(UserTable)
        initUserDB()
    }

}

