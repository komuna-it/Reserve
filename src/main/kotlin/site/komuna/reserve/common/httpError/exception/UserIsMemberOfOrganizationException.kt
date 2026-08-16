package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class UserIsMemberOfOrganizationException(): HttpReserveException(
    HttpStatus.CONFLICT,
    ReserveErrorType.USER_IS_MEMBER_OF_ORGANIZATION,) {
}