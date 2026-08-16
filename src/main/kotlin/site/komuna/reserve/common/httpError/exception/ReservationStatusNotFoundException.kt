package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class ReservationStatusNotFoundException(body: Body): HttpReserveException(
    HttpStatus.NOT_FOUND,
    ReserveErrorType.RESERVATION_STATUS_NOT_FOUND,
    body) {

    class Body(
        val value: String? = null,
    )

    constructor(value: String): this(Body(value))
}