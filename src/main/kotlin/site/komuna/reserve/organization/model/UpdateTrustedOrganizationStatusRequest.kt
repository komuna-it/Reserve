package site.komuna.reserve.organization.model

class UpdateTrustedOrganizationStatusRequest(
    var organizationIds: List<Long>,
    var trusted: Boolean
) {
}