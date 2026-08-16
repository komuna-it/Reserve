package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class RoomNotFoundException(body: Body): HttpReserveException(
    HttpStatus.NOT_FOUND,
    ReserveErrorType.ROOM_NOT_FOUND,
    body
    ) {

    class Body(
        val id: Long?
    )

    constructor(id: Long): this(Body(id))
}