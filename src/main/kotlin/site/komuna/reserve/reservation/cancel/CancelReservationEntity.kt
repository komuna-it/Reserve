package site.komuna.reserve.reservation.cancel

import jakarta.persistence.*
import site.komuna.reserve.reservation.model.ReservationEntity
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime

@Entity
@Table(name = "reservation_cancel")
class CancelReservationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    var reservation: ReservationEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    // Null means that the reservation was approved automatically
    var requestedBy: UserEntity,
    var requestedAt: OffsetDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", nullable = true)
    // Null means that the reservation was approved automatically
    var approvedBy: UserEntity? = null,

    var approvedAt: OffsetDateTime? = null,
) {
}