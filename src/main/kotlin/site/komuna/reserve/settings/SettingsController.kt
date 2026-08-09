package site.komuna.reserve.settings

import io.github.oshai.kotlinlogging.KotlinLogging
import org.hibernate.validator.internal.util.CollectionHelper.newArrayList
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.komuna.reserve.settings.model.ChangeSettingsRequest
import site.komuna.reserve.settings.model.GetSettingsRequest
import site.komuna.reserve.settings.model.SettingsDto
import site.komuna.reserve.settings.model.SettingsEntity
import site.komuna.reserve.settings.model.SettingsKey
import site.komuna.reserve.user.model.UserDto

@RestController
@RequestMapping("/settings")
class SettingsController(
    private val service: SettingsService
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @PatchMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun updateSettings(@RequestBody request: ChangeSettingsRequest, authentication: Authentication): ResponseEntity<List<SettingsDto>> {
        val settings = newArrayList<SettingsDto>()

        request.settings.forEach { requestSetting ->
            val key = requestSetting.key
            val value = requestSetting.value

            logger.info { "Received a request from user id ${authentication.name} to change settings ${key.name} to $value" }

            val response = SettingsDto(service.changeSetting(key.name, value))
            settings.add(response)

            logger.info { "Changed settings ${response.key} to ${response.value}" }
        }

        return ResponseEntity.ok(settings)
    }

    @GetMapping("")
    fun getSettingKey(@RequestBody request: GetSettingsRequest, authentication: Authentication): ResponseEntity<List<SettingsDto>> {
        val requestedBy = authentication.name.toLong()

        val settings = newArrayList<SettingsDto>()

        if (request.all) {
            service.getSettings(requestedBy).forEach {
                settings.add(SettingsDto(it))
            }
        } else if (request.keys != null) {
            request.keys!!.forEach {
                val dto = SettingsDto(service.getSetting(it, requestedBy))
                settings.add(dto)
            }
        }

        return ResponseEntity.ok(settings)
    }
}