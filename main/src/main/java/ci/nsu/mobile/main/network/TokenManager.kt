package ci.nsu.mobile.main.network

import android.content.Context

class TokenManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "auth_token_storage",
        Context.MODE_PRIVATE
    )

    var token: String?
        get() = preferences.getString(KEY_TOKEN, null)
        private set(value) {
            preferences.edit().putString(KEY_TOKEN, value).apply()
        }

    fun saveToken(value: String) {
        token = value
    }

    fun clearToken() {
        preferences.edit().remove(KEY_TOKEN).apply()
    }

    companion object {
        private const val KEY_TOKEN = "jwt_token"
    }
}
