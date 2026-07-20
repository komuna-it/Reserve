package site.komuna.reserve.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import site.komuna.reserve.user.model.UserEntity
import java.util.Optional

@Repository
interface UserRepository: JpaRepository<UserEntity, Long> {
    fun existsUserEntityByEmail(email: String): Boolean
    fun findByEmail(email: String): Optional<UserEntity>

    fun existsByRole(role: Role): Boolean
    fun findByRole(role: Role): List<UserEntity>
}