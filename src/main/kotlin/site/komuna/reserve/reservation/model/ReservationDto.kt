package site.komuna.reserve.reservation.model

import java.time.Duration
import java.time.OffsetDateTime

data class ReservationDto(
    val id: Long?,
    val type: ReservationType,
    val roomId: Long,
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
    val duration: Long,
    val userId: Long? = null,
    val status: ReservationStatus? = null,
    val organizationId: Long? = null,
    val price: Int? = null,
    val paid: Boolean? = false,
    val actorUserId: Long? = null,
) {

    constructor(reservation: ReservationEntity) : this(
        id = reservation.id!!,
        type = reservation.type,
        roomId = reservation.room.id!!,
        startAt = reservation.startAt,
        endAt = reservation.endAt,
        duration = Duration.between(reservation.startAt, reservation.endAt).toMinutes(),
        userId = reservation.user.id!!,
        status = reservation.status,
        organizationId = reservation.organization?.id,
        paid = reservation.paid,
        actorUserId = reservation.actor.id
    )

    constructor(reservation: ReservationEntity, price: Int) : this(
        id = reservation.id!!,
        type = reservation.type,
        roomId = reservation.room.id!!,
        startAt = reservation.startAt,
        endAt = reservation.endAt,
        duration = Duration.between(reservation.startAt, reservation.endAt).toMinutes(),
        userId = reservation.user.id!!,
        status = reservation.status,
        organizationId = reservation.organization?.id,
        paid = reservation.paid,
        price = price,
        actorUserId = reservation.actor.id

    )
}