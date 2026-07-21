package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class CancelReservationDetailsNotConfirmedException(id: Long): ReserveException(HttpStatus.NOT_FOUND, "Cancel reservation details not found for reservation with id: $id") {
}