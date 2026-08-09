package site.komuna.reserve.settings.model

class GetSettingsRequest(
    var keys: List<String>? = null,
    var all: Boolean = false
) {

}