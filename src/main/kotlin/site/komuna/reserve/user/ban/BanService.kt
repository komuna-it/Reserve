package site.komuna.reserve.user.ban

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import site.komuna.reserve.user.UserService
import site.komuna.reserve.user.ban.model.BanDto
import site.komuna.reserve.user.ban.model.BanEntity
import site.komuna.reserve.user.model.UserEntity
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class BanService(
    private val repository: BanRepository
) {

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
        return repository.save(ban)
    }

    fun isUserBanned(id: Long): BanEntity? {
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        val bans = repository.findAllByUserIdAndBanExpiresAfter(id, now)

        return bans.maxByOrNull { it.banExpires }
    }

    fun isUserBanned(user: UserEntity): BanEntity? {
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        return isUserBanned(user.id!!)
    }
}