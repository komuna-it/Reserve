package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class EmailVerificationTokenExpiredException: HttpReserveException(
    HttpStatus.FORBIDDEN,
    ReserveErrorType.EMAIL_VERIFICATION_TOKEN_EXPIRED
) {
}