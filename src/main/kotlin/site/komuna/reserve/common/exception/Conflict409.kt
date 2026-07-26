package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class Conflict409(reason: String): ReserveException(HttpStatus.CONFLICT,reason ) {
}