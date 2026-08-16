package site.komuna.reserve.reservation.model

import site.komuna.reserve.common.httpError.exception.ReservationTypeNotFoundException

enum class ReservationType {
    REHEARSAL,
    RECORDING;

    companion object {
        fun from(value: String): ReservationType {
            return ReservationType.entries.firstOrNull {
                it.name.equals(value, ignoreCase = true)
            } ?: throw ReservationTypeNotFoundException(value)
        }
    }
}