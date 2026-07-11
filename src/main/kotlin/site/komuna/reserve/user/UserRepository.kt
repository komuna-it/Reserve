package site.komuna.reserve.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import site.komuna.reserve.user.model.UserEntity

@Repository
interface UserRepository: JpaRepository<UserEntity, Long> {
    fun existsUserEntityByEmail(email: String): Boolean
    fun findByEmail(email: String): UserEntity?
}