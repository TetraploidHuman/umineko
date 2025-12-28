package org.example.umineko.DB

import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import org.mindrot.jbcrypt.BCrypt
enum class Role { USER, ADMIN }

data class User(
    val id: Long,
    val username: String,
    val passwordHash: String,
    val role: String,
    val tokenVersion: Int
)

object UserTable : Table("users") {
    val id = long("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 60)
    val role = varchar("role", 20) // USER / ADMIN
    val tokenVersion = integer("token_version").default(0)

    override val primaryKey = PrimaryKey(id)
}

suspend fun initUserDB() {
    if (UserTable.selectAll().empty()) {
        UserTable.insert {
            it[username] = "admin"
            it[passwordHash] = BCrypt.hashpw("123456", BCrypt.gensalt())
            it[role] = "ADMIN"
        }
        UserTable.insert {
            it[username] = "user"
            it[passwordHash] = BCrypt.hashpw("123456", BCrypt.gensalt())
            it[role] = "USER"
        }
    }
}

private fun rowToUser(row: ResultRow): User = User(
        id = row[UserTable.id],
        username = row[UserTable.username],
        passwordHash = row[UserTable.passwordHash],
        role = row[UserTable.role],
        tokenVersion = row[UserTable.tokenVersion]
    )


suspend fun findByUsername(username: String): User? = suspendTransaction {
    UserTable.selectAll().where { UserTable.username eq username }
        .firstOrNull()
        ?.let(::rowToUser)
}

suspend fun findById(id: Long): User? = suspendTransaction {
    UserTable.selectAll().where { UserTable.id eq id }
        .firstOrNull()
        ?.let(::rowToUser)
}

suspend fun verifyPassword(user: User, plain: String): Boolean = BCrypt.checkpw(
    plain, user.passwordHash
)

suspend fun increaseTokenVersion(userId: Long) = suspendTransaction {
    UserTable.update({ UserTable.id eq userId }) {
        it[tokenVersion] = UserTable.tokenVersion + 1
    }
}