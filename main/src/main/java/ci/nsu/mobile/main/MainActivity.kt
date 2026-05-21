package ci.nsu.mobile.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import ci.nsu.mobile.main.data.AuthRepository
import ci.nsu.mobile.main.network.ApiClient
import ci.nsu.mobile.main.network.TokenManager
import ci.nsu.mobile.main.ui.AuthApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenManager = TokenManager(applicationContext)
        val apiClient = ApiClient(tokenManager)
        val repository = AuthRepository(apiClient, tokenManager)
        val viewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(repository)
        )[AuthViewModel::class.java]

        setContent {
            AuthApp(viewModel = viewModel)
        }
    }
}
