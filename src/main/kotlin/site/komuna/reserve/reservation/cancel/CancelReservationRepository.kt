package site.komuna.reserve.reservation.cancel

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CancelReservationRepository: JpaRepository<CancelReservationEntity, Long> {

    fun findByReservationId(reservationId: Long): CancelReservationEntity?
}