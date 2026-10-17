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
import java.time.Instant
import java.util.Locale

class AuthService(
    private val users: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtService: JwtService,
    private val refreshTokenService: RefreshTokenService,
    private val refreshSessions: RefreshSessionRepository
) {

    fun register(request: RegisterRequest): UserResponse {
        val email = request.email.trim().lowercase(Locale.ROOT)
        val password = request.password

        require(
            email.length <= 254 &&
                    Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email)
        ) {
            "Invalid email address."
        }

        require(
            password.codePointCount(0, password.length) >= 8 &&
                    password.toByteArray(Charsets.UTF_8).size <= 72
        ) {
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
        val email = request.email.trim().lowercase(Locale.ROOT)
        val password = request.password

        require(
            email.length <= 254 &&
                    Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email) &&
                    password.isNotEmpty()
        ) {
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
        val now = Instant.now()

        refreshSessions.create(
            userId = user.id,
            tokenHash = tokenHash,
            now = now,
            expiresAt = now.plusSeconds(604800)
        )

        return LoginResponse(
            accessToken = jwtService.createAccessToken(user),
            refreshToken = refreshToken
        )
    }

    fun refresh(request: RefreshRequest): AccessTokenResponse {
        require(request.refreshToken.isNotBlank()) {
            "Refresh token must not be blank."
        }

        val tokenHash = refreshTokenService.hash(request.refreshToken)

        val accessToken = refreshSessions.issueAccessTokenIfValid(
            tokenHash = tokenHash,
            now = Instant.now(),
            issueToken = jwtService::createAccessToken
        ) ?: throw invalidRefreshToken()

        return AccessTokenResponse(
            accessToken = accessToken
        )
    }

    fun logout(request: RefreshRequest) {
        require(request.refreshToken.isNotBlank()) {
            "Refresh token must not be blank."
        }

        val tokenHash = refreshTokenService.hash(request.refreshToken)

        val revoked = refreshSessions.revokeIfValid(
            tokenHash = tokenHash,
            now = Instant.now()
        )

        if (!revoked) {
            throw invalidRefreshToken()
        }
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