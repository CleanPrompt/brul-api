package nl.brul.api

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.*
import nl.brul.api.common.error.configureErrorHandling
import nl.brul.api.config.AppConfig
import nl.brul.api.database.DatabaseFactory
import nl.brul.api.features.auth.AuthService
import nl.brul.api.features.auth.repository.ExposedRefreshSessionRepository
import nl.brul.api.features.user.repository.ExposedUserRepository
import nl.brul.api.security.BCryptPasswordHasher
import nl.brul.api.security.JwtService
import nl.brul.api.security.RefreshTokenService
import nl.brul.api.security.configureJwtAuthentication

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val config = AppConfig.fromEnvironment()

    val jwtService = JwtService(
        secret = config.jwtSecret,
        issuer = config.jwtIssuer,
        audience = config.jwtAudience
    )

    DatabaseFactory.init(config.databasePath)

    install(ContentNegotiation) {
        json()
    }

    configureErrorHandling()

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

    configureJwtAuthentication(
        jwtService = jwtService,
        users = userRepository
    )

    configureRouting(authService)
}