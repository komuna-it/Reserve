package site.komuna.reserve.security.token.refresh

import jakarta.persistence.*
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var token: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    var expires: OffsetDateTime,
    var createdAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
) {
}