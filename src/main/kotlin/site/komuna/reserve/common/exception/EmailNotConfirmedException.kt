package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class EmailNotConfirmedException: ReserveException(HttpStatus.FORBIDDEN, "Email was not confirmed") {
}