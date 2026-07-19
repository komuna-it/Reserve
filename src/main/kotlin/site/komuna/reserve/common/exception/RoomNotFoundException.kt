package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class RoomNotFoundException(id: Long): ReserveException(HttpStatus.NOT_FOUND, "Room with id: $id not found") {
}