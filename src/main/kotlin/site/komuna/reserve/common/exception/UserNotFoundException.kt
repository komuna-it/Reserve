package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class UserNotFoundException(id: String): ReserveException(HttpStatus.NOT_FOUND, "User $id not found") {

    constructor(id: Long): this(id.toString())

}