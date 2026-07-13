package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class InvalidRefreshTokenException: ReserveException(HttpStatus.FORBIDDEN, "Invalid refresh token") {
}