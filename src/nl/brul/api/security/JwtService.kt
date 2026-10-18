// src/nl/brul/api/security/JwtService.kt
package nl.brul.api.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import nl.brul.api.features.user.model.User
import java.time.Clock
import java.util.Date

class JwtService(
    secret: String,
    val issuer: String = "brul-api",
    val audience: String = "brul-client",
    private val clock: Clock = Clock.systemUTC()
) {
    companion object {
        const val ACCESS_TOKEN_SECONDS = 900L
    }

    private val algorithm: Algorithm

    init {
        require(secret.toByteArray(Charsets.UTF_8).size >= 32) {
            "BRUL_JWT_SECRET must contain at least 32 UTF-8 bytes."
        }

        require(issuer.isNotBlank()) {
            "JWT issuer must not be blank."
        }

        require(audience.isNotBlank()) {
            "JWT audience must not be blank."
        }

        algorithm = Algorithm.HMAC256(secret)
    }

    fun createAccessToken(user: User): String {
        val now = clock.instant()

        return JWT.create()
            .withSubject(user.id.toString())
            .withClaim("role", user.role.name)
            .withIssuer(issuer)
            .withAudience(audience)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(
                Date.from(now.plusSeconds(ACCESS_TOKEN_SECONDS))
            )
            .sign(algorithm)
    }

    fun createVerifier(): JWTVerifier {
        return JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
    }

    fun isValidTokenLifetime(
        issuedAtMillis: Long,
        expiresAtMillis: Long
    ): Boolean {
        val nowMillis = clock.millis()
        val maxLifetimeMillis = ACCESS_TOKEN_SECONDS * 1000

        if (issuedAtMillis > nowMillis) {
            return false
        }

        if (expiresAtMillis <= issuedAtMillis) {
            return false
        }

        if (expiresAtMillis <= nowMillis) {
            return false
        }

        return expiresAtMillis - issuedAtMillis <= maxLifetimeMillis
    }
}