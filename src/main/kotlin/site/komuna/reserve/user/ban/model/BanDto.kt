package site.komuna.reserve.user.ban.model

import site.komuna.reserve.user.model.UserDto
import java.time.OffsetDateTime

class BanDto(
    var id: Long? = null,
    var user: UserDto,
    var bannedAt: OffsetDateTime,
    var banExpires: OffsetDateTime?, // null if permanent ban
    var reason: String,
    var bannedBy: UserDto,
) {
    constructor(ban: BanEntity, user: UserDto, bannedBy: UserDto) : this(
        ban.id,
        user,
        ban.bannedAt,
        ban.banExpires,
        ban.reason,
        bannedBy
    )
}