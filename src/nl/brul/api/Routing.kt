package nl.brul.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import nl.brul.api.features.auth.AuthService
import nl.brul.api.features.auth.authRoutes
import nl.brul.api.features.user.userRoutes

fun Application.configureRouting(
    authService: AuthService
) {
    routing {
        route("/api/v1") {

            get("/health") {
                call.respond(
                    HttpStatusCode.OK,
                    mapOf("status" to "ok")
                )
            }

            authRoutes(authService)

            userRoutes()
        }
    }
}