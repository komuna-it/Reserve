package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class CannotPerformThatActionException(body: Body): HttpReserveException(
    HttpStatus.FORBIDDEN,
    ReserveErrorType.CANNOT_PERFORM_THAT_ACTION,
    body
) {

    class Body(val message: String)

    constructor(message: String): this(Body(message))
}