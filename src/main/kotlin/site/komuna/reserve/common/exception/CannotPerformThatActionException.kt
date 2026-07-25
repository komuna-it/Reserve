package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class CannotPerformThatActionException(message: String): ReserveException(HttpStatus.FORBIDDEN, message) {
}