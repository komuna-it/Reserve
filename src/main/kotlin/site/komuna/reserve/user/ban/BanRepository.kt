package site.komuna.reserve.user.ban

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import site.komuna.reserve.user.ban.model.BanEntity
import java.time.OffsetDateTime

@Repository
interface BanRepository: JpaRepository<BanEntity, Long> {

    fun findAllByUserIdAndBanExpiresAfter(
        userId: Long,
        banExpiresAfter: OffsetDateTime
    ): List<BanEntity>

}