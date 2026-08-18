package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class CancelReservationDetailsNotConfirmedException(body: Body):
    HttpReserveException(
        HttpStatus.NOT_FOUND,
        ReserveErrorType.RESERVATION_CANCEL_DETAILS_NOT_FOUND
){
    class Body(val reservationId: Long) {
    }

    constructor(reservationId: Long): this(Body(reservationId))
}