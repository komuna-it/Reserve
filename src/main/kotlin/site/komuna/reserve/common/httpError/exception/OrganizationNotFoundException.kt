package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class OrganizationNotFoundException(body: Body): HttpReserveException(
    HttpStatus.NOT_FOUND,
    ReserveErrorType.ORGANIZATION_NOT_FOUND
    , body
    ) {

    class Body(
        val id: Long? = null,
    )

    constructor(id: Long): this(Body(id))
}