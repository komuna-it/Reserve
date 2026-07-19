package site.komuna.reserve.room.model

class RoomDto(
    var id: Long,
    var name: String,
) {
    constructor(roomEntity: RoomEntity) : this(
        roomEntity.id!!,
        roomEntity.name
    )
}