package site.komuna.reserve.reservation.model

import java.time.Duration
import java.time.OffsetDateTime

class ReservationDto(
    val id: Long,
    val type: ReservationType,
    val room: Long,
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
    val duration: Long,
    val reservedBy: Long,
) {

    constructor(reservation: ReservationEntity) : this(
        id = reservation.id!!,
        type = reservation.type,
        room = reservation.room.id!!,
        startAt = reservation.startAt,
        endAt = reservation.endAt,
        duration = Duration.between(reservation.startAt, reservation.endAt).toMinutes(),
        reservedBy = reservation.reservedBy.id!!
    )
}