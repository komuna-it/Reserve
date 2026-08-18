package site.komuna.reserve.settings.model

import jakarta.persistence.*

@Entity
@Table(name = "settings")
class SettingsEntity(

    @Id
    @Enumerated(EnumType.STRING)
    var key: SettingsKey,
    var value: String,
    var isSensitive: Boolean = false,
    var databaseOnly: Boolean = false,
) {
}