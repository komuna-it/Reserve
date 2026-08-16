package site.komuna.reserve.user.model

import jakarta.persistence.*
import site.komuna.reserve.user.Role
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Entity
@Table(name = "users")
class UserEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long?= null,
    var email: String,
    var nick: String,
    var password: String?,
    @Enumerated(EnumType.STRING)
    var role: Role = Role.USER,
    var created: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
    var passwordChanged: OffsetDateTime? = null,
    var trusted: Boolean = false,
    var preferredLanguage: String = "pl"
) {
}