// src/nl/brul/api/features/auth/AuthRoutes.kt
package nl.brul.api.features.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
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