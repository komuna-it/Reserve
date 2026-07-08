package site.komuna.reserve.auth.response

import site.komuna.reserve.security.token.access.AccessToken
import site.komuna.reserve.security.token.refresh.RefreshToken

class LoginResponse(
    val refreshToken: RefreshToken,
    val accessToken: AccessToken,
) {

}