package site.komuna.reserve.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import site.komuna.reserve.reservation.ReservationService
import site.komuna.reserve.settings.SettingsService
import site.komuna.reserve.settings.model.SettingsKey
import java.time.LocalDateTime


@Component
class Scheduler(
    private val settings: SettingsService,
    private val reservationService: ReservationService,
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
        reservationService.sendReminders()
    }
}