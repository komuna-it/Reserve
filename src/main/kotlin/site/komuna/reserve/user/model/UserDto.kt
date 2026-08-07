package site.komuna.reserve.user.model

import site.komuna.reserve.user.Role
import java.time.OffsetDateTime

class UserDto(
    val id: Long,
    val email: String,
    val nick: String,
    val role: Role,
    var trusted: Boolean = false,
    var banned: Boolean = false,
    var bannedUntil: OffsetDateTime? = null
) {
    constructor(
        userEntity: UserEntity,
        banned: Boolean = false,
        bannedUntil: OffsetDateTime? = null
    ) : this(
        id = userEntity.id!!,
        email = userEntity.email,
        nick = userEntity.nick,
        role = userEntity.role,
        trusted = userEntity.trusted,
        banned = banned,
        bannedUntil = bannedUntil
    )
}