package site.komuna.reserve.settings.model

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime

@Entity
@Table(name = "settings")
class SettingsEntity(

    @Id
    @Enumerated(EnumType.STRING)
    var key: SettingsKey,
    var value: String,
    var isSensitive: Boolean = false,
) {
}