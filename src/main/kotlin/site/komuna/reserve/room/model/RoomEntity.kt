package site.komuna.reserve.room.model

import jakarta.persistence.*

@Entity
@Table(name = "rooms")
class RoomEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var name: String,
    var isRecordable: Boolean = false,
) {
}