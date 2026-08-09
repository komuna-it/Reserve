package site.komuna.reserve.settings

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import site.komuna.reserve.settings.model.SettingsEntity
import site.komuna.reserve.settings.model.SettingsKey

@Repository
interface SettingsRepository : JpaRepository<SettingsEntity, SettingsKey> {
}