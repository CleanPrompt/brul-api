// src/nl/brul/api/config/AppConfig.kt

package nl.brul.api.config

data class AppConfig(
    val port: Int,
    val databasePath: String,
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String
) {
    companion object {
        fun fromEnvironment(): AppConfig {
            val portValue = System.getenv("BRUL_PORT")

            val port = if (portValue == null) {
                8080
            } else {
                portValue.toIntOrNull()
                    ?: throw IllegalArgumentException(
                        "BRUL_PORT must be a valid number."
                    )
            }

            require(port in 1..65535) {
                "BRUL_PORT must be between 1 and 65535."
            }

            val databasePath = System.getenv("BRUL_DB_PATH")
                ?: "./data/brul"

            require(databasePath.isNotBlank()) {
                "BRUL_DB_PATH must not be blank."
            }

            val jwtSecret = System.getenv("BRUL_JWT_SECRET")
                ?: error("BRUL_JWT_SECRET is required.")

            require(jwtSecret.toByteArray(Charsets.UTF_8).size >= 32) {
                "BRUL_JWT_SECRET must contain at least 32 UTF-8 bytes."
            }

            val jwtIssuer = System.getenv("BRUL_JWT_ISSUER")
                ?: "brul-api"

            require(jwtIssuer.isNotBlank()) {
                "BRUL_JWT_ISSUER must not be blank."
            }

            val jwtAudience = System.getenv("BRUL_JWT_AUDIENCE")
                ?: "brul-client"

            require(jwtAudience.isNotBlank()) {
                "BRUL_JWT_AUDIENCE must not be blank."
            }

            return AppConfig(
                port = port,
                databasePath = databasePath,
                jwtSecret = jwtSecret,
                jwtIssuer = jwtIssuer,
                jwtAudience = jwtAudience
            )
        }
    }
}