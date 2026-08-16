package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class InvalidRefreshTokenException: HttpReserveException(
    HttpStatus.FORBIDDEN,
    ReserveErrorType.TOKEN_REFRESH_INVALID
) {
}