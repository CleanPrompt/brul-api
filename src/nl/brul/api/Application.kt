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

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    // Centrale configuratie inladen
    val config = AppConfig.fromEnvironment()

    // JWT-service instellen
    val jwtService = JwtService(
        secret = config.jwtSecret,
        issuer = config.jwtIssuer,
        audience = config.jwtAudience
    )

    // Database initialiseren
    DatabaseFactory.init(config.databasePath)

    // JSON-ondersteuning
    install(ContentNegotiation) {
        json()
    }

    // Centrale foutafhandeling
    configureErrorHandling()

    // Repositories en services
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

    // API-routes registreren
    configureRouting(authService)
}