package site.komuna.reserve.organization.organizationMember

import site.komuna.reserve.common.httpError.exception.RoleNotFoundException

enum class OrganizationMemberRole {
    OWNER,
    MEMBER;

    companion object {
        fun from(value: String): OrganizationMemberRole {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true)
            } ?: throw RoleNotFoundException(value)
        }
    }
}