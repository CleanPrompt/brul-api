package nl.brul.api.features.auth

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import nl.brul.api.features.auth.dto.LoginRequest
import nl.brul.api.features.auth.dto.RefreshRequest
import nl.brul.api.features.auth.dto.RegisterRequest

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {

        post("/register") {
            val request = call.receive<RegisterRequest>()
            val response = authService.register(request)
            call.respond(HttpStatusCode.Created, response)
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = authService.login(request)
            call.respond(HttpStatusCode.OK, response)
        }

        post("/refresh") {
            val request = call.receive<RefreshRequest>()
            val response = authService.refresh(request)
            call.respond(HttpStatusCode.OK, response)
        }

        post("/logout") {
            val request = call.receive<RefreshRequest>()
            authService.logout(request)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}