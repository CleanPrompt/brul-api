package nl.brul.api.features.user.repository

import nl.brul.api.database.tables.UsersTable
import nl.brul.api.features.user.model.Role
import nl.brul.api.features.user.model.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class ExposedUserRepository : UserRepository {

    override fun findById(id: Long): User? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.id eq id }
            .singleOrNull()
            ?.toUser()
    }

    override fun findByEmail(email: String): User? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.email eq email }
            .singleOrNull()
            ?.toUser()
    }

    override fun create(
        email: String,
        passwordHash: String
    ): User = transaction {
        val now = Instant.now()

        val id = UsersTable.insert {
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.role] = Role.USER.name
            it[UsersTable.createdAt] = now
        } get UsersTable.id

        User(
            id = id,
            email = email,
            passwordHash = passwordHash,
            role = Role.USER,
            createdAt = now
        )
    }

    private fun ResultRow.toUser(): User = User(
        id = this[UsersTable.id],
        email = this[UsersTable.email],
        passwordHash = this[UsersTable.passwordHash],
        role = Role.valueOf(this[UsersTable.role]),
        createdAt = this[UsersTable.createdAt]
    )
}