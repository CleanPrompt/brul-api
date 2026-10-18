// nl/brul/api/features/user/repository/UserRepository.kt
package nl.brul.api.features.user.repository

import nl.brul.api.features.user.model.User

interface UserRepository {
    fun findById(id: Long): User?
    fun findByEmail(email: String): User?
    fun create(email: String, passwordHash: String): User
}