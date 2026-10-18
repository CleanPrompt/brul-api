// src/nl/brul/api/security/RefreshTokenService.kt
package nl.brul.api.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class RefreshTokenService {

    companion object {
        private const val TOKEN_BYTES = 32
        private const val HASH_ALGORITHM = "SHA-256"
    }

    private val secureRandom = SecureRandom()

    fun generate(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        secureRandom.nextBytes(bytes)

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes)
    }

    fun hash(token: String): String {
        val digest = MessageDigest
            .getInstance(HASH_ALGORITHM)
            .digest(token.toByteArray(Charsets.UTF_8))

        return digest.joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
    }
}