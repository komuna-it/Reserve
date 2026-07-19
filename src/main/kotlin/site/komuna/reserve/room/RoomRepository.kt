package site.komuna.reserve.room

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import site.komuna.reserve.room.model.RoomEntity

@Repository
interface RoomRepository: JpaRepository<RoomEntity, Long> {
}