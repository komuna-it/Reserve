package site.komuna.reserve.user.model

import site.komuna.reserve.user.Role

class UserDto(
    val id: Long,
    val email: String,
    val nick: String,
    val role: Role,
    var trusted: Boolean = false,
    var banned: Boolean = false,
    var preferredLanguage: String = "pl"
) {
    constructor(userEntity: UserEntity, banned: Boolean = false) : this(
        userEntity.id!!,
        userEntity.email,
        userEntity.nick,
        userEntity.role,
        trusted = userEntity.trusted,
        banned = banned,
        preferredLanguage = userEntity.preferredLanguage
    )
}