package site.komuna.reserve.organization.organizationMember.model

import site.komuna.reserve.organization.organizationMember.OrganizationMemberRole

class OrganizationMemberDto(
    var id: Long,
    var organizationId: Long,
    var userId: Long,
    var role: OrganizationMemberRole,
    var nick: String,
    var email: String
) {
    constructor(entity: OrganizationMemberEntity) : this(
        id = entity.id!!,
        organizationId = entity.organization.id!!,
        userId = entity.user.id!!,
        role = entity.role,
        nick = entity.user.nick,
        email = entity.user.email
    )
}