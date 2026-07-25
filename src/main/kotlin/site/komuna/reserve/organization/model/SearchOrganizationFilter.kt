package site.komuna.reserve.organization.model

class SearchOrganizationFilter(
    val organizationId: Long? = null,
    var name: String? = null,
    var ownerId: Long? = null,
    var userId: Long? = null,
    var fetchMembers: Boolean = false,
    val organizationsIds: MutableList<Long> = mutableListOf(),
) {
}