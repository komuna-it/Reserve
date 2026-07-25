package site.komuna.reserve.organization.model

import java.time.OffsetDateTime

class CreateOrganizationRequest(
    var name: String,
    var ownerId: Long?,
    var createdAt: OffsetDateTime? = null,
) {
}