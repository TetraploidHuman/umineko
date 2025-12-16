package org.example.umineko

import io.ktor.http.ContentType
import jdk.internal.joptsimple.internal.Messages.message
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Table.Dual.varchar
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

suspend fun initDB() {
    R2dbcDatabase.connect(
        "r2dbc:h2:file:///C:/Users/LGVP2/IdeaProjects/Umineko/server/src/main/resources/demoTableData;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE"
    )

    suspendTransaction {
        SchemaUtils.create(demoTable)
        if(demoTable.selectAll().empty()){
            demoTable.insert {
                it[message] = "海猫鸣泣之时"
            }
            demoTable.insert {
                it[message] = "うみねこのなく頃に"
            }
        }
    }
}

object demoTable : Table("demoTable") {
    val id = long("id").autoIncrement()
    val message = varchar("message", 255)
    override val primaryKey = PrimaryKey(id,name = "pk_demo_table_id")
}

object demoTableDao{
    suspend fun findMessage(id: Long): String = suspendTransaction {
        val messageRow = demoTable.select(demoTable.id, demoTable.message)
            .where { demoTable.id eq id }
            .firstOrNull() ?: return@suspendTransaction "没找到"

        return@suspendTransaction messageRow[demoTable.message]
    }
}