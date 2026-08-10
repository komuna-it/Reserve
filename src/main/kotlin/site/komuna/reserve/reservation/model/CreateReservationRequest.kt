package site.komuna.reserve.reservation.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import site.komuna.reserve.organization.model.OrganizationEntity
import site.komuna.reserve.room.model.RoomEntity
import site.komuna.reserve.user.model.UserEntity
import java.time.Duration
import java.time.OffsetDateTime

class CreateReservationRequest(
    // Request sent by user
    @field:NotNull(message = "Room ID is required")
    var roomId: Long,

    @field:NotNull(message = "Start at is required")
    var startAt: OffsetDateTime,

    @field:NotNull(message = "Duration is required")
    var duration: Duration,

    var type: ReservationType = ReservationType.REHEARSAL,
    var organizationId: Long? = null,

    // Request organized by system
    var endAt: OffsetDateTime? = null,
    var reservedByUserId: Long? = null,
    var reservedAt: OffsetDateTime? = null,
    var organization: OrganizationEntity? = null,
    var reservedByUser: UserEntity? = null,
    var room: RoomEntity? = null,
) {
}