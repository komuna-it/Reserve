package site.komuna.reserve.organization.organizationMember.model

import site.komuna.reserve.organization.organizationMember.OrganizationMemberRole

class OrganizationMemberDto(
    var id: Long,
    var organizationId: Long,
    var userId: Long,
    var role: OrganizationMemberRole,
) {
    constructor(organizationMemberEntity: OrganizationMemberEntity) : this(
        id = organizationMemberEntity.id!!,
        organizationId = organizationMemberEntity.organization.id!!,
        userId = organizationMemberEntity.user.id!!,
        role = organizationMemberEntity.role
    )
}