package site.komuna.reserve.organization.model

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime

@Entity
@Table(name = "organizations")
class OrganizationEntity(
    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: Long? = null,
    var name: String,
    var created: OffsetDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    var createdBy: UserEntity,
) {
}