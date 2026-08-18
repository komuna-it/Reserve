package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class EmailNotConfirmedException(body: Body): HttpReserveException(
    HttpStatus.FORBIDDEN,
    ReserveErrorType.EMAIL_NOT_CONFIRMED,
    body
) {

    class Body(val email: String) {

    }

    constructor(email: String): this(Body(email))
}