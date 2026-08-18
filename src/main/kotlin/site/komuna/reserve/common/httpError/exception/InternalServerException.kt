package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class InternalServerException(body: Body):
    HttpReserveException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ReserveErrorType.INTERNAL_SERVER_ERROR,
) {
    class Body(
        val message: String = "More details in server logs"
    ) {
    }

    constructor() : this(Body())

}