package ci.nsu.mobile.main.data

import ci.nsu.mobile.main.network.ApiClient
import ci.nsu.mobile.main.network.TokenManager
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AuthRepository(
    private val apiClient: ApiClient,
    private val tokenManager: TokenManager
) {
    private val client = apiClient.client
    private val json: Json = apiClient.json

    suspend fun login(login: String, password: String): Result<UserDto> {
        return runCatching {
            val response = client.post("${ApiClient.BASE_URL}auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(login, password))
            }
            ensureSuccess(response)

            val text = response.bodyAsText()
            val element = parseElementOrNull(text)
            val token = extractToken(element) ?: error("Сервер не вернул токен авторизации.")
            tokenManager.saveToken(token)

            decodeUser(element) ?: UserDto(login = login, token = token)
        }
    }

    suspend fun register(registerRequest: RegisterRequest): Result<Unit> {
        return runCatching {
            val response = client.post("${ApiClient.BASE_URL}auth/register") {
                contentType(ContentType.Application.Json)
                setBody(registerRequest)
            }
            ensureSuccess(response)
        }
    }

    suspend fun getUsers(): Result<List<UserDto>> {
        return runCatching {
            val response = client.get("${ApiClient.BASE_URL}users")
            ensureSuccess(response)
            decodeList(response.bodyAsText(), "users")
        }
    }

    suspend fun getGroups(): Result<List<GroupDto>> {
        return runCatching {
            val response = client.get("${ApiClient.BASE_URL}groups")
            ensureSuccess(response)
            decodeList(response.bodyAsText(), "groups")
        }
    }

    fun logout() {
        tokenManager.clearToken()
    }

    fun hasToken(): Boolean {
        return !tokenManager.token.isNullOrBlank()
    }

    private suspend fun ensureSuccess(response: HttpResponse) {
        if (response.status.isSuccess()) {
            return
        }

        val body = response.bodyAsText()
        val message = extractMessage(parseElementOrNull(body))
            ?: body.takeIf { it.isNotBlank() }
            ?: "Ошибка сервера: ${response.status.value}"
        error(message)
    }

    private inline fun <reified T> decodeList(text: String, preferredKey: String): List<T> {
        val element = parseElementOrNull(text) ?: return emptyList()
        val array = when (element) {
            is JsonArray -> element
            is JsonObject -> {
                val candidates = listOf(preferredKey, "data", "items", "content", "result")
                candidates.firstNotNullOfOrNull { key -> element[key] as? JsonArray }
            }
            else -> null
        } ?: return emptyList()

        return json.decodeFromJsonElement(array)
    }

    private fun decodeUser(element: JsonElement?): UserDto? {
        if (element == null) {
            return null
        }

        return try {
            val userElement = when (element) {
                is JsonObject -> element["user"] ?: element["data"] ?: element
                else -> element
            }
            json.decodeFromJsonElement<UserDto>(userElement)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun extractToken(element: JsonElement?): String? {
        if (element == null) {
            return null
        }

        if (element is JsonPrimitive) {
            return element.contentOrNull
        }

        if (element !is JsonObject) {
            return null
        }

        val directKeys = listOf("token", "accessToken", "jwt", "access_token")
        directKeys.forEach { key ->
            element[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let {
                return it
            }
        }

        val nestedKeys = listOf("data", "user", "result")
        nestedKeys.forEach { key ->
            extractToken(element[key])?.let { return it }
        }

        return null
    }

    private fun extractMessage(element: JsonElement?): String? {
        val objectElement = element as? JsonObject ?: return null
        return listOf("message", "error", "detail")
            .firstNotNullOfOrNull { key ->
                objectElement[key]?.jsonPrimitive?.contentOrNull
            }
    }

    private fun parseElementOrNull(text: String): JsonElement? {
        if (text.isBlank()) {
            return null
        }

        return try {
            json.parseToJsonElement(text)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
