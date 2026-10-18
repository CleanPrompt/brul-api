// nl/brul/api/security/PasswordHasher.kt
package nl.brul.api.security

interface PasswordHasher {
    fun hash(rawPassword: String): String
    fun verify(rawPassword: String, storedHash: String): Boolean
}