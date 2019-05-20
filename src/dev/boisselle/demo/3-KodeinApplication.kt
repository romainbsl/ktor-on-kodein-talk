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
fun Application.kodein() {
    install(KodeinFeature) {
        bind() from singleton { UserService() }
        bind() from singleton { BasicItemService() }

        bind() from scoped(SessionScope).singleton { SessionItemService() }
    }

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

                val itemService: ItemService by kodein().instance()

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
                val userService: UserService by kodein().instance()
                if (userService.check(it.toUser())) UserIdPrincipal(it.name) else null
            }
        }
    }

    routing {
        authenticate("basicAuth") {
            route("/protected") {
                subKodein(allowSilentOverride = true) {
                    bind<ItemService>() with singleton { AuthItemService() }
                }

                get("/user") {
                    val principal = call.principal<UserIdPrincipal>()
                    call.respondText("Hello ${principal?.name ?: "Nobody"}!")
                }
                get("/todolist") {
                    val principal = call.principal<UserIdPrincipal>()
                    if (principal != null) {
                        val itemService: ItemService by kodein().instance()

                        call.respond(
                            HttpStatusCode.OK,
                            itemService.findAllByUserName(principal.name)
                        )
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, "You are not allowed to get there!")
                    }
                }
            }
        }
    }
}

private fun Application.session() {
    data class UserSession(val username: String) : KodeinSession {
        override fun getSessionId() = username
    }

    data class SessionResponse(val apiCallCounter: Int, val items: List<Item>)

    install(Sessions) {
        cookie<UserSession>("SESSION_FEATURE_SESSION_ID", SessionStorageMemory()) {
            cookie.path = "/session" // Specify cookie's path '/' so it can be used in the whole site
        }
    }

    routing {
        authenticate("basicAuth") {
            route("/session") {
                get("/todolist") {
                    val principal = call.principal<UserIdPrincipal>()

                    if (principal != null) {
                        val session = call.sessions.get<UserSession>() ?: UserSession(principal.name)
                        if (call.sessions.get<UserSession>() == null) call.sessions.set(session)

                        val itemService: SessionItemService by kodein().on(session).instance()

                        call.respond(
                            HttpStatusCode.OK,
                            SessionResponse(
                                itemService.apiCallCounter,
                                itemService.findAllByUserName(principal.name)
                            )
                        )
                    }
                }
                get("/clear") {
                    val principal = call.principal<UserIdPrincipal>()
                    call.sessions.clearSessionScope<UserSession>()
                    call.respondText { "${principal?.name ?: "Ghost"}'s Session cleared!" }
                }
            }
        }
    }
}