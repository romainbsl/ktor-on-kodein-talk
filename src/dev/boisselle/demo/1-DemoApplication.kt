package dev.boisselle.demo

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.hello() {
    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }

    routing {
        get("/") {
            call.respondText("Hello Kotlin Meetup !")
        }
    }
}