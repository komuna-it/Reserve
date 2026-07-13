package site.komuna.reserve.user.model

import site.komuna.reserve.user.Role

class UserDto(
    val id: Long,
    val email: String,
    val nick: String,
    val role: Role,
) {
    constructor(userEntity: UserEntity) : this(
        userEntity.id!!,
        userEntity.email,
        userEntity.nick,
        userEntity.role)
}