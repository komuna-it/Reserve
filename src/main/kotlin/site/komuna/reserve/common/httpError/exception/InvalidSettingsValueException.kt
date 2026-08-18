package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class InvalidSettingsValueException(body: Body):
    HttpReserveException(
        HttpStatus.BAD_REQUEST,
        ReserveErrorType.INVALID_SETTINGS_VALUE,
        body
){
    class Body(val key: String, val value: String)

    constructor(key: String, value: String): this(Body(key, value))
}