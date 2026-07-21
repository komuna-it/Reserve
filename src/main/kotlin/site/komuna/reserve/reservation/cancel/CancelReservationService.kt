package site.komuna.reserve.reservation.cancel

import org.springframework.stereotype.Service
import site.komuna.reserve.common.exception.CancelReservationDetailsNotConfirmedException
import site.komuna.reserve.reservation.model.ReservationEntity
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime

@Service
class CancelReservationService(
    private val repository: CancelReservationRepository
) {
    fun saveCancelReservationDetails(reservation: ReservationEntity,
                                     cancelledBy: UserEntity,
                                     canceledAt: OffsetDateTime,
                                     approvedBy: UserEntity?,
                                     approvedAt: OffsetDateTime?) {

        val cancelReservation = CancelReservationEntity(
            reservation = reservation,
            requestedBy = cancelledBy,
            requestedAt = canceledAt,
            approvedBy = approvedBy,
            approvedAt = approvedAt)
        repository.save(cancelReservation)
    }

    fun updateCancelReservationDetails(reservation: ReservationEntity, approvedBy: UserEntity) {
        val cancelReservation = findByReservationId(reservation.id!!)
        cancelReservation.approvedBy = approvedBy
        cancelReservation.approvedAt = OffsetDateTime.now()
        repository.save(cancelReservation)
    }

    fun findByReservationId(id: Long): CancelReservationEntity {
        return repository.findByReservationId(id) ?: throw CancelReservationDetailsNotConfirmedException(id)
    }


}