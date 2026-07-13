package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class VerificationTokenNotFoundException: ReserveException(HttpStatus.NOT_FOUND, "Verification token not found") {
}