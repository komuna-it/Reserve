package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class TokenExpiredException: ReserveException(HttpStatus.FORBIDDEN, "Token was expired") {
}