package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class ReservationStatusNotFoundException(name: String): ReserveException(HttpStatus.NOT_FOUND, "Reservation status $name not found") {
}