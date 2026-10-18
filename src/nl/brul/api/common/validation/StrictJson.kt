
// src/nl/brul/api/common/validation/StrictJson.kt
package nl.brul.api.common.validation

import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

object StrictJson {

    val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
        coerceInputValues = false
        explicitNulls = true
    }

    suspend fun readObject(
        call: ApplicationCall,
        allowedFields: Set<String>
    ): JsonObject {
        require(
            call.request.contentType().match(ContentType.Application.Json)
        ) {
            "Content-Type must be application/json."
        }

        val text = call.receiveText()

        checkDuplicateKeys(text)

        val element = json.parseToJsonElement(text)

        require(element is JsonObject) {
            "Request body must be a JSON object."
        }

        require(element.keys.all { it in allowedFields }) {
            "The request contains unknown fields."
        }

        return element
    }

    suspend fun requireNoBody(call: ApplicationCall) {
        val text = call.receiveText()

        require(text.isEmpty()) {
            "This endpoint does not accept a request body."
        }
    }

    fun requireNumber(
        body: JsonObject,
        field: String
    ): Double? {
        val value = body[field] ?: return null

        require(value is JsonPrimitive && !value.isString) {
            "$field must be a JSON number."
        }

        return value.doubleOrNull
            ?: throw IllegalArgumentException(
                "$field must be a JSON number."
            )
    }

    private fun checkDuplicateKeys(text: String) {
        val scanner = DuplicateKeyScanner(text)
        scanner.scan()
    }

    private class DuplicateKeyScanner(
        private val text: String
    ) {
        private var position = 0

        fun scan() {
            skipWhitespace()
            readValue()
            skipWhitespace()

            require(position == text.length) {
                "Unexpected characters after JSON."
            }
        }

        private fun readValue() {
            skipWhitespace()

            require(position < text.length) {
                "Unexpected end of JSON."
            }

            when (text[position]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                else -> readLiteral()
            }
        }

        private fun readObject() {
            position++
            skipWhitespace()

            val keys = mutableSetOf<String>()

            if (consume('}')) return

            while (true) {
                skipWhitespace()

                require(
                    position < text.length && text[position] == '"'
                ) {
                    "A JSON object key must be a string."
                }

                val key = readString()

                require(keys.add(key)) {
                    "Duplicate JSON field: $key."
                }

                skipWhitespace()
                expect(':')
                readValue()
                skipWhitespace()

                if (consume('}')) return

                expect(',')
            }
        }

        private fun readArray() {
            position++
            skipWhitespace()

            if (consume(']')) return

            while (true) {
                readValue()
                skipWhitespace()

                if (consume(']')) return

                expect(',')
            }
        }

        private fun readString(): String {
            val start = position
            expect('"')

            var escaped = false

            while (position < text.length) {
                val character = text[position++]

                if (escaped) {
                    escaped = false
                } else {
                    when (character) {
                        '\\' -> escaped = true
                        '"' -> {
                            val raw = text.substring(start, position)
                            return json.decodeFromString<String>(raw)
                        }
                    }
                }
            }

            throw IllegalArgumentException(
                "Unterminated JSON string."
            )
        }

        private fun readLiteral() {
            val start = position

            while (
                position < text.length &&
                text[position] !in charArrayOf(
                    ',', '}', ']', ' ', '\t', '\r', '\n'
                )
            ) {
                position++
            }

            require(position > start) {
                "Invalid JSON value."
            }
        }

        private fun skipWhitespace() {
            while (
                position < text.length &&
                text[position] in charArrayOf(
                    ' ', '\t', '\r', '\n'
                )
            ) {
                position++
            }
        }

        private fun consume(character: Char): Boolean {
            if (
                position < text.length &&
                text[position] == character
            ) {
                position++
                return true
            }

            return false
        }

        private fun expect(character: Char) {
            require(consume(character)) {
                "Expected '$character' in JSON."
            }
        }
    }
}
