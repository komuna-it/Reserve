package site.komuna.reserve.organization.model

import org.apache.catalina.User
import site.komuna.reserve.user.model.UserDto

class OrganizationDto(
    var id: Long?,
    var name: String,
    var createdAt: String,
    var members: List<UserDto>? = null,
    var owners: List<UserDto>? = null,
) {

    constructor(organizationEntity: OrganizationEntity) : this(
        organizationEntity.id,
        organizationEntity.name,
        organizationEntity.created.toString(),
    )

    constructor(organizationDto: OrganizationEntity, members: List<UserDto>, owners: List<UserDto>): this(
        id = organizationDto.id,
        name = organizationDto.name,
        createdAt = organizationDto.created.toString(),
        members = members,
        owners = owners
    )
}