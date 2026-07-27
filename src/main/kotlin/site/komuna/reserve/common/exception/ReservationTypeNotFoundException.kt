package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class ReservationTypeNotFoundException(name: String): ReserveException(HttpStatus.NOT_FOUND, "Reservation type $name not found") {
}