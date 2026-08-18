package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class InvalidSettingsKeyException(body: Body):
    HttpReserveException(
        HttpStatus.NOT_FOUND,
        ReserveErrorType.INVALID_SETTINGS_KEY,
        body
){
    class Body(val key: String) {
    }

    constructor(key: String): this(Body(key))
}