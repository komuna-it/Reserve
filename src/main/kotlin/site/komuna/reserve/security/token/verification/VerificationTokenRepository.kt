package site.komuna.reserve.security.token.verification

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import site.komuna.reserve.user.model.UserEntity

@Repository
interface VerificationTokenRepository: JpaRepository<VerificationTokenEntity, Long> {

    fun findByUser(user: UserEntity): VerificationTokenEntity?
    fun findByToken(token: String): VerificationTokenEntity?
    fun deleteByToken(token: String)
}