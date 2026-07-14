package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

open class ReserveException(
    val httpStatus: HttpStatus,
    override val message: String
) : RuntimeException(message)