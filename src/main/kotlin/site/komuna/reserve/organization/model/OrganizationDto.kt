package site.komuna.reserve.organization.model

import site.komuna.reserve.organization.organizationMember.model.OrganizationMemberDto
import java.time.OffsetDateTime

data class OrganizationDto(
    val id: Long?,
    val name: String,
    val createdAt: OffsetDateTime? = null,
    val members: List<OrganizationMemberDto> = emptyList(),
    val owners: List<OrganizationMemberDto> = emptyList(),
    val trusted: Boolean = false,
) {
    constructor(
        organization: OrganizationEntity,
        members: List<OrganizationMemberDto> = emptyList(),
        owners: List<OrganizationMemberDto> = emptyList()
    ) : this(
        id = organization.id,
        name = organization.name,
        createdAt = organization.created,
        trusted = organization.trusted,
        members = members,
        owners = owners
    )
}