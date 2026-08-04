package site.komuna.reserve.user.model

class UpdateTrustedUserStatusRequest(
    var usersIds: List<Long>,
    var trusted: Boolean
) {
}