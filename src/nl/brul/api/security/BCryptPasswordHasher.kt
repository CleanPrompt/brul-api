// src/nl/brul/api/security/BCryptPasswordHasher.kt

package nl.brul.api.security

import org.mindrot.jbcrypt.BCrypt

class BCryptPasswordHasher : PasswordHasher {

    companion object {
        private const val BCRYPT_COST = 12
    }

    override fun hash(rawPassword: String): String {
        return BCrypt.hashpw(
            rawPassword,
            BCrypt.gensalt(BCRYPT_COST)
        )
    }

    override fun verify(
        rawPassword: String,
        storedHash: String
    ): Boolean {
        return try {
            BCrypt.checkpw(rawPassword, storedHash)
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}