package nl.brul.api

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.*
import nl.brul.api.database.DatabaseFactory
import nl.brul.api.features.auth.AuthService
import nl.brul.api.features.auth.repository.ExposedRefreshSessionRepository
import nl.brul.api.features.user.repository.ExposedUserRepository
import nl.brul.api.security.BCryptPasswordHasher
import nl.brul.api.security.JwtService
import nl.brul.api.security.RefreshTokenService

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val jwtSecret = System.getenv("BRUL_JWT_SECRET")
        ?: error("BRUL_JWT_SECRET is required.")

    val jwtIssuer = System.getenv("BRUL_JWT_ISSUER")
        ?: "brul-api"

    val jwtAudience = System.getenv("BRUL_JWT_AUDIENCE")
        ?: "brul-client"

    val jwtService = JwtService(
        secret = jwtSecret,
        issuer = jwtIssuer,
        audience = jwtAudience
    )

    DatabaseFactory.init()

    install(ContentNegotiation) {
        json()
    }

    val userRepository = ExposedUserRepository()
    val passwordHasher = BCryptPasswordHasher()
    val refreshTokenService = RefreshTokenService()
    val refreshSessionRepository = ExposedRefreshSessionRepository()

    val authService = AuthService(
        users = userRepository,
        passwordHasher = passwordHasher,
        jwtService = jwtService,
        refreshTokenService = refreshTokenService,
        refreshSessions = refreshSessionRepository
    )

    configureRouting(authService)
}
