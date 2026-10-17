// src/nl/brul/api/security/JwtAuthentication.kt

package nl.brul.api.security

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.respond
import nl.brul.api.common.error.ApiError
import nl.brul.api.features.user.model.User
import nl.brul.api.features.user.repository.UserRepository

data class AuthenticatedUser(
    val user: User
)

fun Application.configureJwtAuthentication(
    jwtService: JwtService,
    users: UserRepository
) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "brul-api"

            verifier(jwtService.createVerifier())

            validate { credential ->
                val payload = credential.payload

                val userId = payload.subject
                    ?.toLongOrNull()
                    ?: return@validate null

                val tokenRole = payload
                    .getClaim("role")
                    .asString()
                    ?: return@validate null

                val issuedAt = payload.issuedAt
                    ?: return@validate null

                val expiresAt = payload.expiresAt
                    ?: return@validate null

                if (!issuedAt.before(expiresAt)) {
                    return@validate null
                }

                val user = users.findById(userId)
                    ?: return@validate null

                if (user.role.name != tokenRole) {
                    return@validate null
                }

                AuthenticatedUser(user)
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiError(
                        code = "UNAUTHORIZED",
                        message = "Authentication is required."
                    )
                )
            }
        }
    }
}