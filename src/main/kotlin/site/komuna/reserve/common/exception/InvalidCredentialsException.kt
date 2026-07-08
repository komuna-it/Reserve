package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class InvalidCredentialsException: ReserveException(HttpStatus.UNAUTHORIZED, "Invalid credentials") {
}