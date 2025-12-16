package org.example.umineko

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import java.util.concurrent.TimeUnit

suspend fun initDB() {
    R2dbcDatabase.connect(
        "r2dbc:pool:h2:file:///C:/Users/LGVP2/IdeaProjects/Umineko/server/src/main/resources/demoTableData;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE?maxSize=4&initialSize=4"
    )

    suspendTransaction {
        SchemaUtils.create(demoTable)
        if (demoTable.selectAll().empty()) {
            demoTable.insert { it[message] = "海猫鸣泣之时" }
            demoTable.insert { it[message] = "うみねこのなく頃に" }
        }
    }
}

object demoTable : Table("demoTable") {
    val id = long("id").autoIncrement()
    val message = varchar("message", 255)
    override val primaryKey = PrimaryKey(id, name = "pk_demo_table_id")
}

object demoTableDao {
    private val messageCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .buildAsync<Long, String>()

    private val cacheLoaderScope = CoroutineScope(Dispatchers.IO)

    suspend fun findMessage(id: Long): String {
        return messageCache.get(id) { key, _ ->

            cacheLoaderScope.future {
                suspendTransaction {
                    val row = demoTable.select(demoTable.id, demoTable.message)
                        .where { demoTable.id eq key }
                        .firstOrNull()
                    row?.get(demoTable.message) ?: "没找到"
                }
            }
        }.await()
    }

    suspend fun updateMessage(id: Long, newMessage: String) {
        suspendTransaction {
            demoTable.update({ demoTable.id eq id }) {
                it[message] = newMessage
            }
        }
        messageCache.synchronous().invalidate(id)
    }
}