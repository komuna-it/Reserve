package site.komuna.reserve.user.ban

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.actuate.logging.LoggersEndpoint
import org.springframework.stereotype.Service
import site.komuna.reserve.user.ban.model.BanEntity
import site.komuna.reserve.user.model.UserEntity
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class BanService(
    private val repository: BanRepository,
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun banUser(user: UserEntity, by: UserEntity, reason: String, duration: Duration): BanEntity {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val expires = now.plus(duration)

        val ban = BanEntity(
            user = user,
            bannedBy = by,
            reason = reason,
            bannedAt = now,
            banExpires = expires
        )
        val response = repository.save(ban)

        logger.info { "User ${user.email} was banned by ${by.email} for $duration" }
        return response
    }

    fun unbanUser(user: UserEntity, requestedUser: UserEntity) {
        val banEntity = isUserBanned(user) ?: return

        banEntity.banExpires = OffsetDateTime.now(ZoneOffset.UTC)
        repository.save(banEntity)
        logger.info { "User ${user.email} was unbanned by ${requestedUser.email}" }
    }

    fun isUserBanned(id: Long): BanEntity? {
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        val bans = repository.findAllByUserIdAndBanExpiresAfter(id, now)

        return bans.maxByOrNull { it.banExpires }
    }

    fun isUserBanned(user: UserEntity): BanEntity? {
        return isUserBanned(user.id!!)
    }
}