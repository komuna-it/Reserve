package site.komuna.reserve.auth.response

import site.komuna.reserve.security.token.access.AccessToken
import site.komuna.reserve.security.token.refresh.RefreshToken
import site.komuna.reserve.user.model.UserEntity

class LoginResponse(
    val refreshToken: RefreshToken,
    val accessToken: AccessToken,
    val userId: Long,
    val userEmail: String,
    val userNick: String,
    val userRole: String,
) {

    constructor(refreshToken: RefreshToken, accessToken: AccessToken, user: UserEntity) : this(
        refreshToken,
        accessToken,
        userId = user.id!!,
        userEmail = user.email,
        userNick = user.nick,
        userRole = user.role.toString()
    )
}