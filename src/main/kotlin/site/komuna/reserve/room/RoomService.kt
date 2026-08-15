package site.komuna.reserve.room

import org.springframework.stereotype.Service
import site.komuna.reserve.common.exception.RoomNotFoundException
import site.komuna.reserve.reservation.model.ReservationType
import site.komuna.reserve.room.model.RoomDto
import site.komuna.reserve.room.model.RoomEntity
import site.komuna.reserve.room.pricing.PricingService
import site.komuna.reserve.user.UserService

@Service
class RoomService(
    private val repository: RoomRepository,
    private val pricingService: PricingService,
    private val userService: UserService,
) {

    fun getRoom(id: Long): RoomEntity {
        return repository.findById(id).orElseThrow { RoomNotFoundException(id) }
    }

    fun createRoom(roomName: String): RoomDto {
        val room = repository.save(RoomEntity(name = roomName))
        val systemUser = userService.getSystemUser()

        val price = pricingService.initializePrice(room, systemUser)

        return RoomDto(room, price)
    }

    fun getRoomsDto(): List<RoomDto> {
        return repository.findAll().map { room ->
            val pricing = pricingService.getPricing(room)
            RoomDto(room, pricing)
        }.sortedBy { it.name }
    }

    fun getRoomDto(id: Long): RoomDto {
        val room = getRoom(id)

        val pricing = pricingService.getPricing(room)
        return RoomDto(room, pricing)
    }

    fun setNewPrice(roomId: Long, reservationType: String, price: Int, changedBy: Long) {
        val room = getRoom(roomId)
        val changedBy = userService.findById(changedBy)
        val reservationType = ReservationType.from(reservationType)

        pricingService.updatePrice(room, reservationType, price, changedBy)
    }

    fun setRecordable(roomId: Long, value: Boolean): RoomEntity {
        val room = getRoom(roomId)

        room.isRecordable = value
        return repository.save(room)
    }

}