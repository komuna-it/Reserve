package site.komuna.reserve.reservation.confirm

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ConfirmReservationRepository: JpaRepository<ConfirmReservationEntity, Long> {
}