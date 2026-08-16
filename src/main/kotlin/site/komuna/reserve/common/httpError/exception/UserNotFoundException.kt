package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class UserNotFoundException(body: Body): HttpReserveException(
    HttpStatus.NOT_FOUND,
    ReserveErrorType.USER_NOT_FOUND,
    body
) {
    class Body(
        val id: Long? = null,
        val email: String? = null,
    )

    constructor(id: Long): this(Body(id))
    constructor(email: String): this(Body(email = email))
}

