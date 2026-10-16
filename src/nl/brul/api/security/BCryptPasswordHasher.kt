package nl.brul.api.security

import org.mindrot.jbcrypt.BCrypt

class BCryptPasswordHasher : PasswordHasher {

    override fun hash(rawPassword: String): String =
        BCrypt.hashpw(rawPassword, BCrypt.gensalt(12))

    override fun verify(
        rawPassword: String,
        storedHash: String
    ): Boolean =
        BCrypt.checkpw(rawPassword, storedHash)
}