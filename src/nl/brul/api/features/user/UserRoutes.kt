// nl/brul/api/features/user/UserRoutes.kt
package nl.brul.api.features.user

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import nl.brul.api.features.user.dto.UserResponse
import nl.brul.api.security.AuthenticatedUser

fun Route.userRoutes() {

    authenticate("auth-jwt") {

        get("/users/me") {
            val authenticatedUser = call.principal<AuthenticatedUser>()
                ?: return@get call.respond(
                    HttpStatusCode.Unauthorized
                )

            call.respond(
                HttpStatusCode.OK,
                UserResponse.from(authenticatedUser.user)
            )
        }
    }
}