package site.komuna.reserve.reservation.model

import jakarta.persistence.*
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
    @JoinColumn(name = "organization_id")
    var organization: OrganizationEntity?,

    // User for whom the reservation was created
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    var reservedAt: OffsetDateTime,
    var startAt: OffsetDateTime,
    var endAt: OffsetDateTime,

    var paid: Boolean = false,

    @Formula(
        """
        COALESCE(
            (SELECT o.name FROM organizations o WHERE o.id = organization_id),
            (SELECT u.nick FROM users u WHERE u.id = user_id)
        )
        """
    )
    var r: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    var actor: UserEntity,
) {
    constructor(request: CreateReservationRequest) : this(
        room = requireNotNull(request.room),
        type = ReservationType.from(request.type),
        organization = request.organization,
        user = requireNotNull(request.user),
        reservedAt = requireNotNull(request.reservedAt),
        startAt = request.startAt,
        endAt = requireNotNull(request.endAt),
        actor = requireNotNull(request.actor),
    )
}