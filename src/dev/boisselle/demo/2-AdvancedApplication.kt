package dev.boisselle.demo

import com.fasterxml.jackson.databind.*
import dev.boisselle.demo.model.*
import dev.boisselle.demo.service.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import org.kodein.di.generic.*
import org.kodein.di.ktor.*

@Suppress("unused") // Referenced in application.conf
fun Application.advanced() {
    todolist()
    auth()
    session()

    //region pre-configured
    DatabaseFactory.init()
    //endregion
}

private fun Application.todolist() {
    install(ContentNegotiation) {
        jackson {
            configure(SerializationFeature.INDENT_OUTPUT, true)
        }
    }

    routing {
        route("/todolist") {
            get {
                val title: String = call.request.queryParameters["label"] ?: ""

                val itemService = BasicItemService()

                call.respond(
                    HttpStatusCode.OK,
                    when {
                        title.isNotEmpty() -> itemService.findByTitleContains(title)
                        else -> itemService.findAll()
                    }
                )
            }
        }
    }
}

private fun Application.auth() {
    install(Authentication) {
        basic("basicAuth") {
            realm = "Ktor Server"
            validate {
                val userService = UserService()
                if (userService.check(it.toUser())) UserIdPrincipal(it.name) else null
            }
        }
    }

    routing {
        authenticate("basicAuth") {
            get("/protected/user") {
                val principal = call.principal<UserIdPrincipal>()
                call.respondText("Hello ${principal?.name ?: "Nobody"}!")
            }
            get("/protected/todolist") {
                val principal = call.principal<UserIdPrincipal>()
                if (principal != null) {
                    val itemService = AuthItemService()

                    call.respond(
                        HttpStatusCode.OK,
                        itemService.findAllByUserName(principal.name)
                    )
                }
            }
        }
    }
}

private fun Application.session() {
    data class UserSession(val username: String, val counter: Int = 0)
    data class SessionResponse(val apiCallCounter: Int, val items: List<Item>)

    install(Sessions) {
        cookie<UserSession>("SESSION_FEATURE_SESSION_ID", SessionStorageMemory()) {
            cookie.path = "/session" // Specify cookie's path '/' so it can be used in the whole site
        }
    }

    routing {
        authenticate("basicAuth") {
            get("/session/todolist") {
                val principal = call.principal<UserIdPrincipal>()

                if (principal != null) {
                    val session = call.sessions.get<UserSession>() ?: UserSession(principal.name, 1)
                    call.sessions.set(UserSession(principal.name, session.counter + 1))

                    val itemService = BasicItemService()

                    call.respond(
                        HttpStatusCode.OK,
                        SessionResponse(
                            session.counter,
                            itemService.findAllByUserName(principal.name)
                        )
                    )
                }
            }
        }
    }
}