package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class UserNotFoundException(id: Long): ReserveException(HttpStatus.NOT_FOUND, "User $id not found") {
}