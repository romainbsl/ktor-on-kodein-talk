package dev.boisselle.demo.model

import com.fasterxml.jackson.annotation.*
import io.ktor.auth.*
import org.jetbrains.exposed.sql.*
import java.sql.*
import java.time.*

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Item(
    val id: Int = -1,
    val label: String,
    @JsonIgnore val dueTimestamp: Timestamp,
    val username: String?
) {
    val dueDate: String
        get() = dueTimestamp.toLocalDateTime().toLocalDate().toString()
}

data class User(val id: Int = -1, val username: String, val password: String)

fun UserPasswordCredential.toUser() = User(username = this.name, password = this.password)