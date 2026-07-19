package site.komuna.reserve.reservation.model

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import site.komuna.reserve.organization.model.OrganizationEntity
import site.komuna.reserve.room.model.RoomEntity
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime

@Entity
@Table(name = "reservations")
class ReservationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    var room: RoomEntity,

    @Enumerated(EnumType.STRING)
    var type: ReservationType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    // Null in organization means that the reservation is private
    var organization: OrganizationEntity?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_by_user_id", nullable = false)
    var reservedBy: UserEntity,
    var reservedAt: OffsetDateTime,

    var startAt: OffsetDateTime,
    var endAt: OffsetDateTime,
) {

    constructor(request: CreateReservationRequest) : this(
        id = null,
        room = request.room!!,
        type = request.type,
        organization = request.organization,
        reservedBy = request.reservedByUser!!,
        reservedAt = request.reservedAt!!,
        startAt = request.startAt,
        endAt = request.endAt!!,
    )
}