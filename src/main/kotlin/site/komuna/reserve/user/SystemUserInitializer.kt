package site.komuna.reserve.user

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import site.komuna.reserve.user.model.UserEntity

@Component
class SystemUserInitializer(
    private val userRepository: UserRepository
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @Transactional
    @EventListener(ApplicationReadyEvent::class)
    fun initialize() {
        if (userRepository.existsByRole(Role.SYSTEM)) {
            return
        }

        logger.info { "Initializing system user" }

        val systemUser = UserEntity(
            nick = "SYSTEM",
            email = "",
            password = "",
            role = Role.SYSTEM
        )

        userRepository.save(systemUser)
    }
}