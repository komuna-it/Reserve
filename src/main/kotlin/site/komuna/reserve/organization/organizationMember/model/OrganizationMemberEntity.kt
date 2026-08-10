package site.komuna.reserve.organization.organizationMember.model

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import site.komuna.reserve.organization.model.OrganizationEntity
import site.komuna.reserve.organization.organizationMember.OrganizationMemberRole
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime

@Entity
@Table(
    name = "organization_members",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["organization_id", "user_id"])
    ]
)

class   OrganizationMemberEntity (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    var organization: OrganizationEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @Enumerated(EnumType.STRING)
    var role: OrganizationMemberRole,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_user_id", nullable = false)
    var addedBy: UserEntity,

    var addedAt: OffsetDateTime,

    ){
}