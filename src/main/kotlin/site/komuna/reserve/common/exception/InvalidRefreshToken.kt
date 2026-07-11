package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class InvalidRefreshToken: ReserveException(HttpStatus.UNAUTHORIZED, "Invalid refresh token") {
}