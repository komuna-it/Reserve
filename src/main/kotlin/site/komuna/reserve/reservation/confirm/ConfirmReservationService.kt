package site.komuna.reserve.reservation.confirm

import org.springframework.stereotype.Service
import site.komuna.reserve.reservation.model.ReservationEntity
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class ConfirmReservationService(
    private val repository: ConfirmReservationRepository
) {
    fun saveConfirmReservationDetails(reservation: ReservationEntity, approvedBy: UserEntity) {
        val confirmDetails = repository.findByReservation(reservation)
            ?: ConfirmReservationEntity(reservation = reservation)

        confirmDetails.approvedBy = approvedBy
        confirmDetails.approvedAt = OffsetDateTime.now(ZoneOffset.UTC)

        repository.save(confirmDetails)
    }

    fun saveRejectReservationDetails(reservation: ReservationEntity, rejectedBy: UserEntity) {
        val confirmDetails = repository.findByReservation(reservation)
            ?: ConfirmReservationEntity(reservation = reservation)

        confirmDetails.approvedBy = rejectedBy
        confirmDetails.approvedAt = OffsetDateTime.now(ZoneOffset.UTC)

        repository.save(confirmDetails)
    }
}