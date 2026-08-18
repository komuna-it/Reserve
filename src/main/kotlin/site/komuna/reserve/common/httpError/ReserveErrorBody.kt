package site.komuna.reserve.common.httpError

import org.springframework.http.HttpStatus

class ReserveErrorBody(
    val httpStatus: HttpStatus,
    val type: ReserveErrorType,
    val body: Any? = null

) {

    constructor(error: HttpReserveException): this(error.httpStatus, error.type, error.body)
}