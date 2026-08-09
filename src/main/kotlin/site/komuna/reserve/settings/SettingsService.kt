package site.komuna.reserve.settings

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.transaction.Transactional
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import site.komuna.reserve.common.exception.ReserveException
import site.komuna.reserve.settings.model.SettingsEntity
import site.komuna.reserve.settings.model.SettingsKey
import site.komuna.reserve.user.Role
import site.komuna.reserve.user.UserService
import java.util.concurrent.ConcurrentHashMap

@Service
class SettingsService(
    private val repository: SettingsRepository,
    private val userService: UserService,
    private val cache: ConcurrentHashMap<SettingsKey, String> = ConcurrentHashMap<SettingsKey, String>()
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun changeSetting(key: String, value: String): SettingsEntity {
        val settingsKey = SettingsKey.valueOf(key)

        validateSetting(settingsKey, value)

        val entity = repository.findById(settingsKey).get()

        entity.value = value
        val response = repository.save(entity)
        
        cache[settingsKey] = value
        applyChanges(response)

        return response
    }

    fun validateSetting(key: SettingsKey, value: String) {
        if (!key.validator.matches(value)) {
            throw ReserveException(HttpStatus.BAD_REQUEST, "Invalid value for setting ${key.name}")
        }
    }

    fun getSetting(key: String, requestedBy: Long): SettingsEntity {

        val user = userService.findById(requestedBy)

        val key = SettingsKey.valueOf(key)

        if(key.isSensitive && (user.role != Role.ADMIN && user.role != Role.MANAGER)) {
            logger.warn { "User with id ${user.id} tried to get sensitive settings" }

            throw ReserveException(HttpStatus.FORBIDDEN, "You are not allowed to get sensitive settings")
        }

        return repository.findById(key).get()
    }

    fun getSettings(requestedBy: Long): List<SettingsEntity> {
        val user = userService.findById(requestedBy)

        val response = repository.findAll()

        return if (user.role == Role.ADMIN || user.role == Role.MANAGER) {
            response
        } else {
            response.filter { !it.isSensitive }
        }
    }

    fun getStringValue(key: SettingsKey): String {
        return cache.computeIfAbsent(key) {
            repository.findById(key).get().value
        }
    }

    fun getIntValue(key: SettingsKey): Int {
        return getStringValue(key).toInt()
    }

    fun getBooleanValue(key: SettingsKey): Boolean {
        return getStringValue(key).toBoolean()
    }

    /**
     * Method checks if settings that were changed impacting a running application
     * Then apply to keep it working
     */
    fun applyChanges(setting: SettingsEntity) {

    }

    /**
     * Initialize settings cache and database
     * If settings not exist in a database, then create it based on a default value
     * If settings exist in a database, then get it from a database and put it in a cache
     */
    @Transactional
    @PostConstruct
    fun initialize() {
        logger.info { "Initializing settings" }

        SettingsKey.entries.forEach { key ->
            if (!repository.existsById(key)) {
                logger.info { "Did not found settings with key: ${key.name} in database, creating it with default value: ${key.defaultValue}" }

                repository.save(
                    SettingsEntity(
                        key = key,
                        value = key.defaultValue,
                        isSensitive = key.isSensitive
                    )
                )

                cache[key] = key.defaultValue
            }
            else {
                cache[key] = repository.findById(key).get().value

                if(cache[key]?.contains("password") == true) {
                    logger.info { "Found settings with key: ${key.name} in database, value: PASSWORD_HIDDEN" }
                }
                else {
                    logger.info { "Found settings with key: ${key.name} in database, value: ${cache[key]}" }
                }
            }
        }

        logger.info { "Settings initialized" }
    }
}