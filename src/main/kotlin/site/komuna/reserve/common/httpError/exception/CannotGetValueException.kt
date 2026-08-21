package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class CannotGetValueException(body: Body): HttpReserveException(
    HttpStatus.FORBIDDEN,
    ReserveErrorType.CANNOT_GET_THAT_SETTING_VALUE
) {
    class Body(
        val key: String
    )

    constructor(key: String): this(Body(key))
}