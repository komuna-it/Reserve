package site.komuna.reserve.room.pricing

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import site.komuna.reserve.reservation.model.ReservationType
import site.komuna.reserve.room.model.RoomEntity
import site.komuna.reserve.room.pricing.model.PricingEntity
import java.math.BigDecimal
import java.time.OffsetDateTime

@Repository
interface PricingRepository: JpaRepository<PricingEntity, Long> {

    fun findFirstByRoomAndReservationTypeOrderByValidFromDesc(room: RoomEntity, reservationType: ReservationType): PricingEntity?

    @Query(
        value = """
        SELECT 
            p.price * EXTRACT(EPOCH FROM (
                CAST(:endAt AS timestamptz) - CAST(:startAt AS timestamptz)
            )) / 3600
        FROM pricing p
        WHERE p.room_id = :roomId
          AND p.reservation_type = :reservationType
          AND p.valid_from <= CAST(:startAt AS timestamptz)
        ORDER BY p.valid_from DESC
        LIMIT 1
    """,
        nativeQuery = true
    )
    fun calculatePrice(
        @Param("roomId") roomId: Long,
        @Param("reservationType") reservationType: String,
        @Param("startAt") startAt: OffsetDateTime,
        @Param("endAt") endAt: OffsetDateTime,
    ): Int?
}