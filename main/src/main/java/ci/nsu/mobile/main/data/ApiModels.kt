package ci.nsu.mobile.main.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupDto(
    @SerialName("groupId") val groupId: Int? = null,
    @SerialName("id") val id: Int? = null,
    @SerialName("groupName") val groupName: String? = null,
    @SerialName("name") val name: String? = null
) {
    val actualId: Int
        get() = groupId ?: id ?: 0

    val displayName: String
        get() = groupName ?: name ?: "Группа $actualId"
}

@Serializable
data class PersonDto(
    val firstName: String? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    val birthDate: String? = null,
    val gender: String? = null,
    val groupId: Int? = null,
    val group: GroupDto? = null
) {
    val fullName: String
        get() = listOf(lastName, firstName, middleName)
            .filterNot { it.isNullOrBlank() }
            .joinToString(" ")
            .ifBlank { "Имя не указано" }
}

@Serializable
data class UserDto(
    @SerialName("userId") val userId: Int? = null,
    @SerialName("id") val id: Int? = null,
    val login: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val roleId: Int? = null,
    val token: String? = null,
    val accessToken: String? = null,
    val jwt: String? = null,
    val person: PersonDto? = null
) {
    val actualId: Int
        get() = userId ?: id ?: 0

    val displayName: String
        get() = person?.fullName?.takeIf { it.isNotBlank() } ?: login ?: "Пользователь $actualId"
}

@Serializable
data class LoginRequest(
    val login: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val login: String,
    val password: String,
    val email: String,
    val phoneNumber: String,
    val roleId: Int = 1,
    val authAllowed: Boolean = true,
    val person: PersonDto
)
