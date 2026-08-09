package site.komuna.reserve.room.model

class RoomDto(
    var id: Long,
    var name: String,
    var isRecordable: Boolean = false,
    var pricing: Map<String, Int>? = null,
) {
    constructor(roomEntity: RoomEntity, pricing: Map<String, Int>?) : this(
        roomEntity.id!!,
        roomEntity.name,
        roomEntity.isRecordable,
        pricing
    )

    constructor(roomEntity: RoomEntity) : this(roomEntity, null)
}