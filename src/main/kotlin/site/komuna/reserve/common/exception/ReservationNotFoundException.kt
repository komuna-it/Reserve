package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class ReservationNotFoundException(id: Long): ReserveException(HttpStatus.NOT_FOUND, "Reservation with id: $id not found") {
}