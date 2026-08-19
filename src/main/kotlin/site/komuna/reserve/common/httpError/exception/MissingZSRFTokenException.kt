package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class MissingZSRFTokenException: HttpReserveException(
    HttpStatus.FORBIDDEN,
    ReserveErrorType.MISSING_ZSRF_TOKEN) {
}