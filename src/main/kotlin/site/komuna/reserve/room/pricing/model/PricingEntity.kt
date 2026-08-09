package site.komuna.reserve.room.pricing.model

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import site.komuna.reserve.reservation.model.ReservationType
import site.komuna.reserve.room.model.RoomEntity
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Entity
@Table(name = "pricing")
class PricingEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    var room: RoomEntity,

    @Enumerated(EnumType.STRING)
    var reservationType: ReservationType,

    var price: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    var changedBy: UserEntity? = null,

    var validFrom: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)
) {
}