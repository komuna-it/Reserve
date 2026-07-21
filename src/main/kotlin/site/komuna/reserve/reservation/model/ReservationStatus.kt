package site.komuna.reserve.reservation.model

enum class ReservationStatus {
    CREATED,
    CONFIRMED,
    REQUESTED_CANCELLATION,
    CANCELLED,
    REJECTED_CANCELLATION,
}