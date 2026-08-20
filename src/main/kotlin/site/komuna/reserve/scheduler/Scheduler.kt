package site.komuna.reserve.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import site.komuna.reserve.organization.OrganizationService
import site.komuna.reserve.reservation.ReservationService
import site.komuna.reserve.settings.SettingsService
import site.komuna.reserve.settings.model.SettingsKey
import java.time.LocalDateTime


@Component
class Scheduler(
    private val settings: SettingsService,
    private val reservationService: ReservationService,
    private val organizationService: OrganizationService,
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    /**
     * Method runs every hour to check is there any scheduled task to execute
     */
    @Scheduled(cron = "0 0 * * * *")
    fun scheduler() {
        logger.info { "Scheduled task executed" }
        logger.info { "Settings: ${settings.getIntValue(SettingsKey.RESERVATION_REMINDER_HOUR)}" }

        sendRemindersToUsers()
        unassignDeletedUsers()
    }

    fun sendRemindersToUsers() {

        val currentHour = LocalDateTime.now().hour
        val reminderHour = settings.getIntValue(SettingsKey.RESERVATION_REMINDER_HOUR)

        logger.info { "Current hour: $currentHour" }
        logger.info { "Reminder hour: $reminderHour" }

        if(currentHour != reminderHour) {
            return
        }

        logger.info { "Sending tomorrow's reservation reminders to users" }
        reservationService.emitReservationReminders()
    }

    fun unassignDeletedUsers() {
        val currentHour = LocalDateTime.now().hour

        if(currentHour != 21) {
            return
        }

        logger.info { "Unassigning deleted users" }

        organizationService.unassignOrphanUsers()
    }
}