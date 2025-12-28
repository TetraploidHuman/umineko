package org.example.umineko.DB

import kotlinx.coroutines.flow.firstOrNull
import org.example.umineko.DSL.cacheEvict
import org.example.umineko.DSL.cachePut
import org.example.umineko.DSL.cacheable
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update

suspend fun initTestDB() {
    if (demoTable.selectAll().empty()) {
        demoTable.insert { it[message] = "海猫鸣泣之时" }
        demoTable.insert { it[message] = "うみねこのなく頃に" }
    }
}

object demoTable : Table("demoTable") {
    val id = long("id").autoIncrement()
    val message = varchar("message", 255)
    override val primaryKey = PrimaryKey(id, name = "pk_demo_table_id")
}

object demoTableDao {
    suspend fun findMessage(id: Long): String = cacheable(name = "demoTable.message", key = id) {
        suspendTransaction {
            val row = demoTable.select(demoTable.message).where { demoTable.id eq id }.firstOrNull()
                ?: return@suspendTransaction "没找到"

            row[demoTable.message]
        }
    }

    //此函数在更新数据后使对应 key 的缓存失效
    suspend fun updateMessage(id: Long, msg: String) {
        suspendTransaction {
            demoTable.update({ demoTable.id eq id }) {
                it[message] = msg
            }
        }

        cacheEvict(name = "demoTable.message", key = id)
    }

    //从数据库重新加载并写入缓存
    suspend fun forceRefresh(id: Long): String = cachePut(name = "demoTable.message", key = id) { findMessage(id) }

}