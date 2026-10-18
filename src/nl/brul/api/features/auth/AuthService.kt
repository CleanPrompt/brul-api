// src/nl/brul/api/features/auth/AuthService.kt
package nl.brul.api.features.auth

import io.ktor.http.HttpStatusCode
import nl.brul.api.common.error.AppException
import nl.brul.api.features.auth.dto.AccessTokenResponse
import nl.brul.api.features.auth.dto.LoginRequest
import nl.brul.api.features.auth.dto.LoginResponse
import nl.brul.api.features.auth.dto.RefreshRequest
import nl.brul.api.features.auth.dto.RegisterRequest
import nl.brul.api.features.auth.repository.RefreshSessionRepository
import nl.brul.api.features.user.dto.UserResponse
import nl.brul.api.features.user.repository.UserRepository
import nl.brul.api.security.JwtService
import nl.brul.api.security.PasswordHasher
import nl.brul.api.security.RefreshTokenService
import java.time.Clock
import java.util.Locale

class AuthService(
    private val users: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtService: JwtService,
    private val refreshTokenService: RefreshTokenService,
    private val refreshSessions: RefreshSessionRepository,
    private val clock: Clock = Clock.systemUTC()
) {
    companion object {
        private const val MAX_EMAIL_LENGTH = 254
        private const val MIN_PASSWORD_CODE_POINTS = 8
        private const val MAX_PASSWORD_BYTES = 72
        private const val REFRESH_TOKEN_SECONDS = 604800L

        private val EMAIL_REGEX =
            Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }

    fun register(request: RegisterRequest): UserResponse {
        val email = normalizeEmail(request.email)
        val password = request.password

        require(isValidEmail(email)) {
            "Invalid email address."
        }

        require(isValidPassword(password)) {
            "Invalid password length."
        }

        if (users.findByEmail(email) != null) {
            throw AppException(
                status = HttpStatusCode.Conflict,
                code = "EMAIL_ALREADY_EXISTS",
                message = "An account with this email address already exists."
            )
        }

        val passwordHash = passwordHasher.hash(password)
        val user = users.create(email, passwordHash)

        return UserResponse.from(user)
    }

    fun login(request: LoginRequest): LoginResponse {
        val email = normalizeEmail(request.email)
        val password = request.password

        require(isValidEmail(email) && password.isNotEmpty()) {
            "Invalid login request."
        }

        val user = users.findByEmail(email)

        if (
            user == null ||
            !passwordHasher.verify(password, user.passwordHash)
        ) {
            throw InvalidCredentialsException()
        }

        val refreshToken = refreshTokenService.generate()
        val tokenHash = refreshTokenService.hash(refreshToken)
        val now = clock.instant()

        refreshSessions.create(
            userId = user.id,
            tokenHash = tokenHash,
            now = now,
            expiresAt = now.plusSeconds(REFRESH_TOKEN_SECONDS)
        )

        return LoginResponse(
            accessToken = jwtService.createAccessToken(user),
            refreshToken = refreshToken
        )
    }

    fun refresh(request: RefreshRequest): AccessTokenResponse {
        val tokenHash = validatedRefreshTokenHash(request.refreshToken)

        val accessToken = refreshSessions.issueAccessTokenIfValid(
            tokenHash = tokenHash,
            now = clock.instant(),
            issueToken = jwtService::createAccessToken
        ) ?: throw invalidRefreshToken()

        return AccessTokenResponse(
            accessToken = accessToken
        )
    }

    fun logout(request: RefreshRequest) {
        val tokenHash = validatedRefreshTokenHash(request.refreshToken)

        val revoked = refreshSessions.revokeIfValid(
            tokenHash = tokenHash,
            now = clock.instant()
        )

        if (!revoked) {
            throw invalidRefreshToken()
        }
    }

    private fun normalizeEmail(email: String): String {
        return email.trim().lowercase(Locale.ROOT)
    }

    private fun isValidEmail(email: String): Boolean {
        return email.length <= MAX_EMAIL_LENGTH &&
                EMAIL_REGEX.matches(email)
    }

    private fun isValidPassword(password: String): Boolean {
        val codePoints = password.codePointCount(0, password.length)
        val bytes = password.toByteArray(Charsets.UTF_8).size

        return codePoints >= MIN_PASSWORD_CODE_POINTS &&
                bytes <= MAX_PASSWORD_BYTES
    }

    private fun validatedRefreshTokenHash(refreshToken: String): String {
        require(refreshToken.isNotBlank()) {
            "Refresh token must not be blank."
        }

        return refreshTokenService.hash(refreshToken)
    }

    private fun invalidRefreshToken(): AppException {
        return AppException(
            status = HttpStatusCode.Unauthorized,
            code = "INVALID_REFRESH_TOKEN",
            message = "Invalid or expired refresh token."
        )
    }
}

class InvalidCredentialsException : RuntimeException(
    "Invalid email or password."
)