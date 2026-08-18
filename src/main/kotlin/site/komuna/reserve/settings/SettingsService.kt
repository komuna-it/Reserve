package site.komuna.reserve.settings

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import site.komuna.reserve.common.httpError.exception.CannotGetValueException
import site.komuna.reserve.common.httpError.exception.InvalidSettingsValueException
import site.komuna.reserve.settings.model.SettingsEntity
import site.komuna.reserve.settings.model.SettingsKey
import site.komuna.reserve.user.Role
import site.komuna.reserve.user.model.UserEntity
import java.util.concurrent.ConcurrentHashMap

@Service
class SettingsService(
    private val repository: SettingsRepository,
    private val cache: ConcurrentHashMap<SettingsKey, SettingsEntity> = ConcurrentHashMap<SettingsKey, SettingsEntity>()
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
        
        cache[settingsKey] = response
        applyChanges(response)

        return response
    }

    fun validateSetting(key: SettingsKey, value: String) {
        if (!key.validator.matches(value)) {
            throw InvalidSettingsValueException(key.name, value)
        }
    }

    fun getSetting(key: String, requestedBy: UserEntity): SettingsEntity {

        val key = SettingsKey.valueOf(key)

        if (key.databaseOnly) {
            logger.warn { "User with id ${requestedBy.id} tried to get database only settings" }
            throw CannotGetValueException(key.name)
        }

        if(key.isSensitive && (requestedBy.role != Role.ADMIN && requestedBy.role != Role.MANAGER)) {
            logger.warn { "User with id ${requestedBy.id} tried to get sensitive settings" }
            throw CannotGetValueException(key.name)
        }

        return repository.findById(key).get()
    }

    /**
     * Returns all settings that are not sensitive or database only
     */
    fun getAllSettings(requestedBy: UserEntity): List<SettingsEntity> {
        val response = ArrayList<SettingsEntity>()

        if (requestedBy.role == Role.ADMIN || requestedBy.role == Role.MANAGER) {
            cache.forEach { (key, value) ->
                if(!value.databaseOnly) {
                    response.add(value)
                }
            }
        }
        else {
            cache.forEach { (key, value) ->
                if(!value.databaseOnly && !value.isSensitive) {
                    response.add(value)
                }
            }
            return response
        }

        return response
    }

    fun getStringValue(key: SettingsKey): String {
        val value = cache.computeIfAbsent(key) {
            repository.findById(key).get()
        }

        return value.value
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
                // Populate database with default values
                logger.info { "Did not found settings with key: ${key.name} in database, creating it with default value: ${key.defaultValue}" }

                val value = repository.save(
                    SettingsEntity(
                        key = key,
                        value = key.defaultValue,
                        isSensitive = key.isSensitive,
                        databaseOnly = key.databaseOnly
                    )
                )

                cache[key] = value
            }
            else {
                // Get settings from database and put it in a cache
                cache[key] = repository.findById(key).get()

                if(cache[key]?.value!!.contains("password")) {
                    logger.info { "Found settings with key: ${key.name} in database, value: PASSWORD_HIDDEN" }
                }
                else {
                    logger.info { "Found settings with key: ${key.name} in database, value: ${cache[key]?.value}" }
                }
            }
        }

        logger.info { "Settings initialized" }
    }
}