package site.komuna.reserve.user.ban.model

import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime

class BanDto(
    var id: Long? = null,
    var user: UserEntity,
    var bannedAt: OffsetDateTime,
    var banExpires: OffsetDateTime?, // null if permanent ban
    var reason: String,
    var bannedBy: UserEntity,
) {
    constructor(ban: BanEntity) : this(
        ban.id,
        ban.user,
        ban.bannedAt,
        ban.banExpires,
        ban.reason,
        ban.bannedBy
    )
}