package site.komuna.reserve.common.httpError

import org.springframework.http.HttpStatus

open class HttpReserveException(
    val httpStatus: HttpStatus,
    val type: ReserveErrorType,
    val body: Any? = null
): RuntimeException() {

}