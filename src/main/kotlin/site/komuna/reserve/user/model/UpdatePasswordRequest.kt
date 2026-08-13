package site.komuna.reserve.user.model

class UpdatePasswordRequest(
    val currentPassword: String,
    val newPassword: String
){

}