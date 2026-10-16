package nl.brul.api

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import nl.brul.api.features.auth.AuthService
import nl.brul.api.features.auth.authRoutes

fun Application.configureRouting(authService: AuthService) {
    routing {
        route("/api/v1") {
            get("/health") {
                call.respondText("Brul API is online!")
            }

            authRoutes(authService)
        }
    }
}