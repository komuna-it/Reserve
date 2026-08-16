package site.komuna.reserve.organization.model

import jakarta.persistence.*
import jakarta.persistence.GenerationType.IDENTITY
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Entity
@Table(name = "organizations")
class OrganizationEntity(
    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: Long? = null,
    var name: String,
    var created: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    var createdBy: UserEntity,
    var trusted: Boolean = false,
) {
}