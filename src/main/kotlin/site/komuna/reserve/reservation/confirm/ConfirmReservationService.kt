package site.komuna.reserve.reservation.confirm

import org.springframework.stereotype.Service
import site.komuna.reserve.reservation.confirm.model.ConfirmReservationEntity
import site.komuna.reserve.reservation.model.ReservationEntity
import site.komuna.reserve.user.model.UserEntity

@Service
class ConfirmReservationService(
    private val repository: ConfirmReservationRepository
) {
    fun saveConfirmReservationDetails(reservation: ReservationEntity, approvedBy: UserEntity) {
        val confirmReservation = ConfirmReservationEntity(reservation = reservation, approvedBy = approvedBy)
        repository.save(confirmReservation)
    }
}