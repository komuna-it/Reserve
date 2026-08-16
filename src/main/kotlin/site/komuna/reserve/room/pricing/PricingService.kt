package site.komuna.reserve.room.pricing

import org.springframework.stereotype.Service
import site.komuna.reserve.reservation.model.ReservationEntity
import site.komuna.reserve.reservation.model.ReservationType
import site.komuna.reserve.room.model.RoomEntity
import site.komuna.reserve.room.pricing.model.PricingEntity
import site.komuna.reserve.user.model.UserEntity

@Service
class PricingService(
    private val repository: PricingRepository
) {
    fun getPricing(room: RoomEntity): Map<String, Int> {

        val pricing = mutableMapOf<String, Int>()

        ReservationType.entries.forEach { type ->
            val pricingEntity = repository.findFirstByRoomAndReservationTypeOrderByValidFromDesc(room, type)

            if (pricingEntity != null) {
                pricing[type.name] = pricingEntity.price
            }
        }

        return pricing.toMap()
    }

    fun initializePrice(room: RoomEntity, changedBy: UserEntity): Map<String, Int> {
        val price = 50

        repository.save(PricingEntity(null, room, ReservationType.REHEARSAL, price, changedBy))
        return mapOf(ReservationType.REHEARSAL.name to price)
    }

    fun updatePrice(room: RoomEntity, reservationType: ReservationType, price: Int, changedBy: UserEntity) {
        repository.save(PricingEntity(null, room, reservationType, price, changedBy))
    }

    fun getPrice(reservation: ReservationEntity): Int {
        val roomId = reservation.room.id!!
        val reservationType = reservation.type
        val startAt = reservation.startAt
        val endAt = reservation.endAt

        return repository.calculatePrice(roomId, reservationType.name, startAt, endAt) ?: 0
    }
}