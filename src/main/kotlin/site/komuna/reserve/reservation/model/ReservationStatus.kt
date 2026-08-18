package site.komuna.reserve.reservation.model

import site.komuna.reserve.common.httpError.exception.ReservationStatusNotFoundException

enum class ReservationStatus {
    CREATED,
    CONFIRMED,
    REJECTED,
    REQUESTED_CANCELLATION,
    CANCELLED,
    REJECTED_CANCELLATION;

    companion object {
        fun from(value: String): ReservationStatus {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true)
            } ?: throw ReservationStatusNotFoundException(value)
        }
    }
}