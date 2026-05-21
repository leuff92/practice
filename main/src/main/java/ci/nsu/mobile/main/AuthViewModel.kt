package ci.nsu.mobile.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ci.nsu.mobile.main.data.AuthRepository
import ci.nsu.mobile.main.data.GroupDto
import ci.nsu.mobile.main.data.PersonDto
import ci.nsu.mobile.main.data.RegisterRequest
import ci.nsu.mobile.main.data.UserDto
import kotlinx.coroutines.launch

enum class AuthScreen {
    LOGIN,
    REGISTER,
    USERS
}

data class RegisterForm(
    val firstName: String = "",
    val lastName: String = "",
    val middleName: String = "",
    val birthDate: String = "",
    val gender: String = "Мужской",
    val selectedGroupId: Int = 0,
    val login: String = "",
    val password: String = "",
    val email: String = "",
    val phoneNumber: String = ""
)

data class AuthUiState(
    val screen: AuthScreen = AuthScreen.LOGIN,
    val login: String = "",
    val password: String = "",
    val registerForm: RegisterForm = RegisterForm(),
    val users: List<UserDto> = emptyList(),
    val groups: List<GroupDto> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    var uiState by mutableStateOf(
        AuthUiState(
            screen = if (repository.hasToken()) AuthScreen.USERS else AuthScreen.LOGIN
        )
    )
        private set

    init {
        if (uiState.screen == AuthScreen.USERS) {
            loadUsers()
        }
    }

    fun updateLogin(value: String) {
        uiState = uiState.copy(login = value, message = null)
    }

    fun updatePassword(value: String) {
        uiState = uiState.copy(password = value, message = null)
    }

    fun updateRegisterForm(transform: (RegisterForm) -> RegisterForm) {
        uiState = uiState.copy(registerForm = transform(uiState.registerForm), message = null)
    }

    fun openLogin() {
        uiState = uiState.copy(screen = AuthScreen.LOGIN, message = null, password = "")
    }

    fun openRegister() {
        uiState = uiState.copy(screen = AuthScreen.REGISTER, message = null)
        loadGroups()
    }

    fun login() {
        val login = uiState.login.trim()
        val password = uiState.password

        if (login.isBlank() || password.isBlank()) {
            uiState = uiState.copy(message = "Введите логин и пароль.")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, message = null)
            val result = repository.login(login, password)
            result.fold(
                onSuccess = {
                    uiState = uiState.copy(
                        isLoading = false,
                        screen = AuthScreen.USERS,
                        password = "",
                        message = "Вход выполнен успешно."
                    )
                    loadUsers()
                },
                onFailure = { error ->
                    uiState = uiState.copy(isLoading = false, message = error.toRussianMessage())
                }
            )
        }
    }

    fun register() {
        val validationError = validateRegisterForm()
        if (validationError != null) {
            uiState = uiState.copy(message = validationError)
            return
        }

        val form = uiState.registerForm
        val request = RegisterRequest(
            login = form.login.trim(),
            password = form.password,
            email = form.email.trim(),
            phoneNumber = form.phoneNumber.trim(),
            person = PersonDto(
                firstName = form.firstName.trim(),
                lastName = form.lastName.trim(),
                middleName = form.middleName.trim().ifBlank { null },
                birthDate = form.birthDate.trim(),
                gender = form.gender,
                groupId = form.selectedGroupId
            )
        )

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, message = null)
            val result = repository.register(request)
            result.fold(
                onSuccess = {
                    uiState = uiState.copy(
                        isLoading = false,
                        screen = AuthScreen.LOGIN,
                        login = form.login.trim(),
                        password = "",
                        registerForm = RegisterForm(),
                        message = "Регистрация выполнена. Теперь войдите в систему."
                    )
                },
                onFailure = { error ->
                    uiState = uiState.copy(isLoading = false, message = error.toRussianMessage())
                }
            )
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, message = null)
            val result = repository.getUsers()
            result.fold(
                onSuccess = { users ->
                    uiState = uiState.copy(
                        isLoading = false,
                        users = users,
                        message = if (users.isEmpty()) "Список пользователей пуст." else null
                    )
                },
                onFailure = { error ->
                    uiState = uiState.copy(isLoading = false, message = error.toRussianMessage())
                }
            )
        }
    }

    fun loadGroups() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, message = null)
            val result = repository.getGroups()
            result.fold(
                onSuccess = { groups ->
                    val selectedId = uiState.registerForm.selectedGroupId
                        .takeIf { id -> groups.any { it.actualId == id } }
                        ?: groups.firstOrNull()?.actualId
                        ?: 0
                    uiState = uiState.copy(
                        isLoading = false,
                        groups = groups,
                        registerForm = uiState.registerForm.copy(selectedGroupId = selectedId),
                        message = if (groups.isEmpty()) "Список групп пуст." else null
                    )
                },
                onFailure = { error ->
                    uiState = uiState.copy(isLoading = false, message = error.toRussianMessage())
                }
            )
        }
    }

    fun logout() {
        repository.logout()
        uiState = uiState.copy(
            screen = AuthScreen.LOGIN,
            password = "",
            users = emptyList(),
            message = "Вы вышли из системы."
        )
    }

    fun clearMessage() {
        uiState = uiState.copy(message = null)
    }

    private fun validateRegisterForm(): String? {
        val form = uiState.registerForm

        return when {
            form.lastName.isBlank() -> "Введите фамилию."
            form.firstName.isBlank() -> "Введите имя."
            form.birthDate.isBlank() -> "Введите дату рождения."
            form.selectedGroupId <= 0 -> "Выберите группу."
            form.login.isBlank() -> "Введите логин."
            form.password.length < 4 -> "Пароль должен быть не короче 4 символов."
            form.email.isBlank() || !form.email.contains("@") -> "Введите корректный email."
            form.phoneNumber.isBlank() -> "Введите телефон."
            else -> null
        }
    }

    private fun Throwable.toRussianMessage(): String {
        val text = message?.takeIf { it.isNotBlank() } ?: "Неизвестная ошибка."
        return when {
            text.contains("timeout", ignoreCase = true) -> "Сервер не отвечает. Проверьте сеть."
            text.contains("unexpected end", ignoreCase = true) -> "Соединение с сервером было прервано."
            text.contains("socket", ignoreCase = true) -> "Ошибка сетевого соединения."
            text.contains("unable to resolve host", ignoreCase = true) -> "Не удалось найти сервер."
            text.contains("failed to connect", ignoreCase = true) -> "Не удалось подключиться к серверу."
            text.contains("connect", ignoreCase = true) -> "Не удалось подключиться к серверу."
            text.contains("cleartext", ignoreCase = true) -> "HTTP-соединение не разрешено настройками безопасности."
            text.contains("401") || text.contains("403") -> "Нет доступа. Проверьте логин, пароль или токен."
            else -> text
        }
    }
}

class AuthViewModelFactory(
    private val repository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Неизвестный класс ViewModel")
    }
}
