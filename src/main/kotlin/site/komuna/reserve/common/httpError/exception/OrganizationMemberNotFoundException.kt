package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class OrganizationMemberNotFoundException(body: Body): HttpReserveException(
    HttpStatus.NOT_FOUND,
    ReserveErrorType.USER_IS_NOT_MEMBER_OF_ORGANIZATION,
    body) {

    class Body(
        val userId: Long? = null,
        val organizationId: Long? = null
    )

    constructor(userId: Long, organizationId: Long): this(Body(userId, organizationId))
}