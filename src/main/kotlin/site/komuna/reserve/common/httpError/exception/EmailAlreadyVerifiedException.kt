package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class EmailAlreadyVerifiedException: HttpReserveException(
    HttpStatus.CONFLICT,
    ReserveErrorType.EMAIL_ALREADY_VERIFIED
) {
}