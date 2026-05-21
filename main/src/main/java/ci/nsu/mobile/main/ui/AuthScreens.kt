package ci.nsu.mobile.main.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ci.nsu.mobile.main.AuthScreen
import ci.nsu.mobile.main.AuthUiState
import ci.nsu.mobile.main.AuthViewModel
import ci.nsu.mobile.main.RegisterForm
import ci.nsu.mobile.main.data.GroupDto
import ci.nsu.mobile.main.data.UserDto

@Composable
fun AuthApp(viewModel: AuthViewModel) {
    val state = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = when (state.screen) {
                        AuthScreen.LOGIN -> "Вход"
                        AuthScreen.REGISTER -> "Регистрация"
                        AuthScreen.USERS -> "Пользователи"
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF8F9FC)
                ) {
                    when (state.screen) {
                        AuthScreen.LOGIN -> LoginScreen(
                            state = state,
                            onLoginChanged = viewModel::updateLogin,
                            onPasswordChanged = viewModel::updatePassword,
                            onLoginClick = viewModel::login,
                            onRegisterClick = viewModel::openRegister
                        )

                        AuthScreen.REGISTER -> RegisterScreen(
                            state = state,
                            onFormChanged = viewModel::updateRegisterForm,
                            onRegisterClick = viewModel::register,
                            onBackClick = viewModel::openLogin,
                            onReloadGroups = viewModel::loadGroups
                        )

                        AuthScreen.USERS -> UsersScreen(
                            state = state,
                            onRefreshClick = viewModel::loadUsers,
                            onLogoutClick = viewModel::logout
                        )
                    }
                }

                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(title: String) {
    TopAppBar(
        title = { Text(title) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF304C89),
            titleContentColor = Color.White
        )
    )
}

@Composable
private fun LoginScreen(
    state: AuthUiState,
    onLoginChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        ScreenTitle("Авторизация")
        Text(
            text = "Введите логин и пароль для работы с API.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = state.login,
            onValueChange = onLoginChanged,
            label = { Text("Логин") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChanged,
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onLoginClick,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Войти")
        }
        TextButton(
            onClick = onRegisterClick,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Нет аккаунта? Зарегистрироваться")
        }
    }
}

@Composable
private fun RegisterScreen(
    state: AuthUiState,
    onFormChanged: ((RegisterForm) -> RegisterForm) -> Unit,
    onRegisterClick: () -> Unit,
    onBackClick: () -> Unit,
    onReloadGroups: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        ScreenTitle("Новый аккаунт")
        Text(
            text = "Заполните данные пользователя. Роль будет назначена автоматически.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        RegisterTextField(
            value = state.registerForm.lastName,
            label = "Фамилия",
            onValueChange = { onFormChanged { form -> form.copy(lastName = it) } }
        )
        RegisterTextField(
            value = state.registerForm.firstName,
            label = "Имя",
            onValueChange = { onFormChanged { form -> form.copy(firstName = it) } }
        )
        RegisterTextField(
            value = state.registerForm.middleName,
            label = "Отчество",
            onValueChange = { onFormChanged { form -> form.copy(middleName = it) } }
        )
        RegisterTextField(
            value = state.registerForm.birthDate,
            label = "Дата рождения, например 2000-01-31",
            onValueChange = { onFormChanged { form -> form.copy(birthDate = it) } }
        )
        GenderDropdown(
            value = state.registerForm.gender,
            onValueChange = { gender ->
                onFormChanged { form -> form.copy(gender = gender) }
            }
        )
        GroupDropdown(
            groups = state.groups,
            selectedGroupId = state.registerForm.selectedGroupId,
            onValueChange = { groupId ->
                onFormChanged { form -> form.copy(selectedGroupId = groupId) }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onReloadGroups,
                enabled = !state.isLoading
            ) {
                Text("Обновить группы")
            }
        }

        RegisterTextField(
            value = state.registerForm.login,
            label = "Логин",
            onValueChange = { onFormChanged { form -> form.copy(login = it) } }
        )
        RegisterTextField(
            value = state.registerForm.password,
            label = "Пароль",
            isPassword = true,
            onValueChange = { onFormChanged { form -> form.copy(password = it) } }
        )
        RegisterTextField(
            value = state.registerForm.email,
            label = "Email",
            keyboardType = KeyboardType.Email,
            onValueChange = { onFormChanged { form -> form.copy(email = it) } }
        )
        RegisterTextField(
            value = state.registerForm.phoneNumber,
            label = "Телефон",
            keyboardType = KeyboardType.Phone,
            onValueChange = { onFormChanged { form -> form.copy(phoneNumber = it) } }
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onBackClick,
                enabled = !state.isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Назад")
            }
            Button(
                onClick = onRegisterClick,
                enabled = !state.isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Зарегистрироваться")
            }
        }
    }
}

@Composable
private fun UsersScreen(
    state: AuthUiState,
    onRefreshClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        ScreenTitle("Список пользователей")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onRefreshClick,
                enabled = !state.isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Обновить")
            }
            TextButton(
                onClick = onLogoutClick,
                enabled = !state.isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Выйти")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (state.users.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Пользователи не загружены.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.users, key = { it.actualId }) { user ->
                    UserCard(user = user)
                }
            }
        }
    }
}

@Composable
private fun UserCard(user: UserDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Логин: ${user.login ?: "не указан"}")
            Text("Email: ${user.email ?: "не указан"}")
            user.phoneNumber?.takeIf { it.isNotBlank() }?.let {
                Text("Телефон: $it")
            }
            val groupName = user.person?.group?.displayName
                ?: user.person?.groupId?.let { "Группа $it" }
            groupName?.let {
                Text("Группа: $it")
            }
        }
    }
}

@Composable
private fun ScreenTitle(value: String) {
    Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun RegisterTextField(
    value: String,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = keyboardType
        ),
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderDropdown(
    value: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val genders = listOf("Мужской", "Женский")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Пол") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            genders.forEach { gender ->
                DropdownMenuItem(
                    text = { Text(gender) },
                    onClick = {
                        onValueChange(gender)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupDropdown(
    groups: List<GroupDto>,
    selectedGroupId: Int,
    onValueChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedGroup = groups.firstOrNull { it.actualId == selectedGroupId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (groups.isNotEmpty()) {
                expanded = !expanded
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        OutlinedTextField(
            value = selectedGroup?.displayName ?: "Группы не загружены",
            onValueChange = {},
            readOnly = true,
            label = { Text("Группа") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            enabled = groups.isNotEmpty(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = groups.isNotEmpty())
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            groups.forEach { group ->
                DropdownMenuItem(
                    text = { Text(group.displayName) },
                    onClick = {
                        onValueChange(group.actualId)
                        expanded = false
                    }
                )
            }
        }
    }
}
