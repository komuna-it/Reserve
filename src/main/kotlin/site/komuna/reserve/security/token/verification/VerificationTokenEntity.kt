package site.komuna.reserve.security.token.verification

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime

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
    var createdAt: OffsetDateTime,

    ) {

}