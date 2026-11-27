package site.komuna.reserve.statistics

import org.springframework.data.jpa.repository.JpaRepository
import site.komuna.reserve.reservation.model.ReservationEntity
import java.time.OffsetDateTime

interface StatisticsRepository : JpaRepository<ReservationEntity, Long> {

    fun findAllByStartAtGreaterThanEqualAndStartAtLessThan(
        startAt: OffsetDateTime,
        endAt: OffsetDateTime
    ): List<ReservationEntity>
}