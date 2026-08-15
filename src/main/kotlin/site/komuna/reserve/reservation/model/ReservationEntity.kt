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
import org.hibernate.annotations.Formula
import site.komuna.reserve.organization.model.OrganizationEntity
import site.komuna.reserve.room.model.RoomEntity
import site.komuna.reserve.user.Role
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime

@Entity
@Table(name = "reservations")
class ReservationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Enumerated(EnumType.STRING)
    var status: ReservationStatus = ReservationStatus.CREATED,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    var room: RoomEntity,

    @Enumerated(EnumType.STRING)
    var type: ReservationType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = true)
    // Null in organization means that the reservation is private
    var organization: OrganizationEntity?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_by_user_id", nullable = false)
    var reservedBy: UserEntity,
    var reservedAt: OffsetDateTime,

    var startAt: OffsetDateTime,
    var endAt: OffsetDateTime,

    var paid: Boolean = false,

    @Formula("COALESCE((SELECT o.name FROM organizations o WHERE o.id = organization_id), (SELECT u.nick FROM users u WHERE u.id = reserved_by_user_id))")
    var r: String? = null // string wyciągający nazwę zespołu lub nazwę usera, jeśli to prywatna rezerwacja (dla sortowania z fronta)
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
    ) {
        if(reservedBy.role == Role.ADMIN) {
            status = ReservationStatus.CONFIRMED
        }
    }
}