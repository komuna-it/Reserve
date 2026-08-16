package site.komuna.reserve.user.ban.model

import jakarta.persistence.*
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime

@Entity
@Table(name = "bans")
class BanEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    var bannedAt: OffsetDateTime,
    var banExpires: OffsetDateTime,
    var reason: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_by_user_id", nullable = false)
    var bannedBy: UserEntity,
){
}