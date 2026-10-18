// src/nl/brul/api/Application.kt
package nl.brul.api

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import nl.brul.api.common.error.configureErrorHandling
import nl.brul.api.config.AppConfig
import nl.brul.api.database.DatabaseFactory
import nl.brul.api.features.auth.AuthService
import nl.brul.api.features.auth.repository.ExposedRefreshSessionRepository
import nl.brul.api.features.help.HelpService
import nl.brul.api.features.help.repository.ExposedHelpRepository
import nl.brul.api.features.report.ReportService
import nl.brul.api.features.report.repository.ExposedReportRepository
import nl.brul.api.features.user.repository.ExposedUserRepository
import nl.brul.api.features.vote.VoteService
import nl.brul.api.features.vote.repository.ExposedVoteRepository
import nl.brul.api.security.BCryptPasswordHasher
import nl.brul.api.security.JwtService
import nl.brul.api.security.RefreshTokenService
import nl.brul.api.security.configureJwtAuthentication

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
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
        json(
            Json {
                ignoreUnknownKeys = false
                isLenient = false
                explicitNulls = true
            }
        )
    }

    configureErrorHandling()

    val userRepository = ExposedUserRepository()
    val refreshSessionRepository = ExposedRefreshSessionRepository()
    val reportRepository = ExposedReportRepository()
    val voteRepository = ExposedVoteRepository()
    val helpRepository = ExposedHelpRepository()

    val passwordHasher = BCryptPasswordHasher()
    val refreshTokenService = RefreshTokenService()

    val authService = AuthService(
        users = userRepository,
        passwordHasher = passwordHasher,
        jwtService = jwtService,
        refreshTokenService = refreshTokenService,
        refreshSessions = refreshSessionRepository
    )

    val reportService = ReportService(
        reports = reportRepository
    )

    val voteService = VoteService(
        votes = voteRepository
    )

    val helpService = HelpService(
        helps = helpRepository
    )

    configureJwtAuthentication(
        jwtService = jwtService,
        users = userRepository
    )

    configureRouting(
        authService = authService,
        reportService = reportService,
        voteService = voteService,
        helpService = helpService
    )
}
