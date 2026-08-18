package site.komuna.reserve.user

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import site.komuna.reserve.user.model.UserEntity
import java.util.*

@Repository
interface UserRepository: JpaRepository<UserEntity, Long> {
    fun existsUserEntityByEmail(email: String): Boolean
    fun findByEmail(email: String): Optional<UserEntity>
    fun existsByRole(role: Role): Boolean
    fun findByRole(role: Role): List<UserEntity>
    fun findByNickNot(nick: String, pageable: Pageable): Page<UserEntity>
}