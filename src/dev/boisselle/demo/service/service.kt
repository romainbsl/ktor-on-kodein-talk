package dev.boisselle.demo.service

import dev.boisselle.demo.model.*
import dev.boisselle.demo.model.DatabaseFactory.dbQuery
import dev.boisselle.demo.model.Items.id
import org.jetbrains.exposed.sql.*
import java.sql.*

//region Item Service
interface ItemService {
    suspend fun findByTitleContains(str: String) = dbQuery {
        Items.select { (Items.label like "%$str%") }
            .mapNotNull { it.extractItem() }
    }

    suspend fun findAll() = dbQuery {
        Items.selectAll().mapNotNull { it.extractItem() }
    }

    suspend fun findByUsernameAndTitleContains(username: String, str: String) = dbQuery {
        Items.select {
            ((Items.username.isNull()) or
                    (Items.username eq username)) and
                    (Items.label like "%$str%")
        }.mapNotNull { it.extractItem() }
    }

    suspend fun findAllByUserName(username: String) = dbQuery {
        Items.select {
            (Items.username.isNull()) or
                    (Items.username eq username)
        }.mapNotNull { it.extractItem() }
    }

    fun ResultRow.extractItem(): Item =
        Item(
            id = this[id],
            label = this[Items.label],
            dueTimestamp = Timestamp(this[Items.dueTimestamp]),
            username = this[Items.username]
        )
}

class BasicItemService : ItemService
class AuthItemService : ItemService {
    override suspend fun findAll() = dbQuery {
        Items.select { (Items.username.isNull()) }
            .mapNotNull { it.extractItem() }
    }
}

class SessionItemService : ItemService {
    private var _apiCallCounter = 0
    val apiCallCounter
        get() = ++_apiCallCounter
}
//endregion

//region User Service
class UserService {
    suspend fun check(user: User): Boolean = dbQuery {
        Users.select {
            (Users.username eq user.username) and
                    (Users.password eq user.password)
        }.mapNotNull { it.extractUser() }
            .singleOrNull() != null
    }

    private fun ResultRow.extractUser(): User =
        User(
            id = this[Users.id],
            username = this[Users.username],
            password = this[Users.password]
        )
}
//endregion