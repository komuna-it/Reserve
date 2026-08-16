package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType
import java.time.OffsetDateTime

class UserBannedException(body: Body): HttpReserveException(
    HttpStatus.FORBIDDEN,
    ReserveErrorType.USER_BANNED,
    body
    ) {

    class Body(val bannedUntil: OffsetDateTime? = null)

    constructor(bannedUntil: OffsetDateTime) : this(Body(bannedUntil))

}