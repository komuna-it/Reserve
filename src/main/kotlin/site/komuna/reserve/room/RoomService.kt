package site.komuna.reserve.room

import org.springframework.stereotype.Service
import site.komuna.reserve.common.exception.RoomNotFoundException
import site.komuna.reserve.room.model.RoomEntity

@Service
class RoomService(
    private val repository: RoomRepository
) {

    fun getRooms(): List<RoomEntity> {
        return repository.findAll()
    }

    fun getRoom(id: Long): RoomEntity {
        return repository.findById(id).orElseThrow { RoomNotFoundException(id) }
    }

    fun createRoom(roomName: String): RoomEntity {
        return repository.save(RoomEntity(name = roomName))
    }

}