package site.komuna.reserve.settings.model

class SettingsDto(
    var key: SettingsKey,
    var value: String,
) {

    constructor(entity: SettingsEntity) : this(
        key = entity.key,
        value = if (entity.key == SettingsKey.MAIL_SERVER_PASSWORD) {
            "PASSWORD_HIDDEN"
        } else {
            entity.value
        }
    )
}