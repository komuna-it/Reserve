package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class EmailAlreadyTakenException: ReserveException(HttpStatus.CONFLICT, "Email already taken") {
}