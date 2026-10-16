package nl.brul.api.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import nl.brul.api.features.user.model.User
import java.time.Clock
import java.util.Date

class JwtService(
    secret: String,
    private val issuer: String = "brul-api",
    private val audience: String = "brul-client",
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
            .withExpiresAt(Date.from(now.plusSeconds(ACCESS_TOKEN_SECONDS)))
            .sign(algorithm)
    }
}