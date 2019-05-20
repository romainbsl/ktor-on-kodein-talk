package dev.boisselle.demo.model

import com.zaxxer.hikari.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.*
import java.sql.*
import java.time.*

object DatabaseFactory {
    fun init() {
        Database.connect(hikari())
        transaction {
            create(Users)
            val romainId = Users.insert {
                it[username] = "romain"
                it[password] = "pwd"
            } get Users.username
            val leslieId = Users.insert {
                it[username] = "leslie"
                it[password] = "pwd"
            } get Users.username

            create(Items)
            Items.insert {
                it[label] = "Pickup the kids !"
                it[dueTimestamp] = Timestamp.valueOf(LocalDateTime.now().plusDays(1)).time
            }
            Items.insert {
                it[label] = "Find a doctor for the kids"
                it[dueTimestamp] = Timestamp.valueOf(LocalDateTime.now().plusDays(5)).time
            }
            Items.insert {
                it[label] = "Prepare the Kotlin Meetup presentation"
                it[dueTimestamp] = Timestamp.valueOf(LocalDateTime.now().minusDays(1)).time
                it[username] = romainId
            }
            Items.insert {
                it[label] = "Conf call with client"
                it[dueTimestamp] = Timestamp.valueOf(LocalDateTime.now().plusDays(1)).time
                it[username] = romainId
            }
            Items.insert {
                it[label] = "Find some new zero waste inspiration"
                it[dueTimestamp] = Timestamp.valueOf(LocalDateTime.now().minusDays(3)).time
                it[username] = leslieId
            }
            Items.insert {
                it[label] = "Call Mom!"
                it[dueTimestamp] = Timestamp.valueOf(LocalDateTime.now().plusDays(5)).time
                it[username] = leslieId
            }

        }
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.h2.Driver"
        config.jdbcUrl = "jdbc:h2:mem:test"
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    suspend fun <T> dbQuery(block: () -> T): T =
        withContext(Dispatchers.IO) {
            transaction { block() }
        }
}

object Items : Table() {
    val id = integer("id").primaryKey().autoIncrement()
    val label = varchar("label", 50)
    val dueTimestamp = long("dueTimestamp")
    val username = (varchar("username", 50) references Users.username).nullable()
}

object Users : Table() {
    val id = integer("id").primaryKey().autoIncrement()
    val username = varchar("username", 50)
    val password = varchar("password", 50)
}