package ci.nsu.mobile.main.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.logging.HttpLoggingInterceptor

class ApiClient(tokenManager: TokenManager) {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    val client = HttpClient(OkHttp) {
        expectSuccess = false

        engine {
            config {
                addInterceptor(AuthInterceptor(tokenManager))
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    }
                )
            }
        }

        install(ContentNegotiation) {
            json(json)
        }
    }

    companion object {
        const val BASE_URL = "http://192.168.200.160:8080/api/"
    }
}
