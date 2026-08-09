package site.komuna.reserve.user.model

import com.fasterxml.jackson.annotation.JsonInclude
import site.komuna.reserve.user.Role
import site.komuna.reserve.user.ban.model.BanDto
import java.time.OffsetDateTime

@JsonInclude(JsonInclude.Include.NON_NULL) // nie wysylaj banDto jesli nie jest zbanowany
data class UserDto(
    val id: Long,
    val email: String,
    val nick: String,
    val role: Role,

    var trusted: Boolean = false,
    var preferredLanguage: String = "pl",
    val banDto: BanDto? = null
) {
    constructor(
        userEntity: UserEntity,
        banDto: BanDto? = null
    ) : this(
        id = userEntity.id!!,
        email = userEntity.email,
        nick = userEntity.nick,
        role = userEntity.role,
        trusted = userEntity.trusted,
        preferredLanguage = userEntity.preferredLanguage,
        banDto = banDto
    )
}