package site.komuna.reserve.reservation.model

import java.time.OffsetDateTime

class SearchReservationsFilter(
    val userId: Long? = null,
    val reservationId: Long? = null,
    var reservedBy: Long? = null,
    var organizationsId: MutableList<Long> = mutableListOf(),
    val future : Boolean = false,
    val private : Boolean = false,
    val roomId: Long? = null,
    val startAtAfter: OffsetDateTime? = null,
    val startAtBefore: OffsetDateTime? = null,
    val status: MutableList<ReservationStatus> = mutableListOf(),
    val type: MutableList<ReservationType> = mutableListOf(),
) {
}