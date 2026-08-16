package site.komuna.reserve.security.token.verification

import jakarta.persistence.*
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Entity
@Table(name = "verification_tokens")
class VerificationTokenEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,
    var token: String,
    var expires: OffsetDateTime,
    var createdAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),

    ) {

}