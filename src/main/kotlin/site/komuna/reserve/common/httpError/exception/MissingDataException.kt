package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class MissingDataException(body: Body):
    HttpReserveException(
        HttpStatus.BAD_REQUEST,
        ReserveErrorType.MISSING_DATA,
        body
) {
    class Body(val missingField: String)

    constructor(missingField: String): this(Body(missingField))
}