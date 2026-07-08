package site.komuna.reserve.user

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import site.komuna.reserve.auth.request.RegisterRequest
import site.komuna.reserve.user.model.UserDto
import site.komuna.reserve.user.model.UserEntity

@Service
class UserService(
    private val repository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun createUser(request: RegisterRequest): UserDto {

        val email = request.email
        val nick = request.name
        val password = passwordEncoder.encode(request.password)
        val role = Role.USER

        val savedUser = repository.save(UserEntity(
            email = email,
            nick = nick,
            password = password,
            role = role
        ))

        return UserDto(savedUser)
    }

    fun isEmailTaken(email: String): Boolean {
        return repository.existsUserEntityByEmail(email)
    }

    fun findByEmail(email: String): UserEntity? {
        return repository.findByEmail(email)
    }
}