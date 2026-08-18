package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class OrganizationLastOwnerException(): HttpReserveException(
    HttpStatus.CONFLICT,
    ReserveErrorType.ORGANIZATION_LAST_OWNER,) {
}