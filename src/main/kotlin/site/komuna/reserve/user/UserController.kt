package site.komuna.reserve.user

import io.github.oshai.kotlinlogging.KotlinLogging
import org.hibernate.validator.internal.util.CollectionHelper.newArrayList
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.komuna.reserve.user.ban.model.BanDto
import site.komuna.reserve.user.ban.model.BanRequest
import site.komuna.reserve.user.model.UpdateTrustedUserStatusRequest
import site.komuna.reserve.user.model.UserDto

@RestController
@RequestMapping("/users")
class UserController(
    private val service: UserService
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @GetMapping("/all")
    fun getUsers(): ResponseEntity<List<UserDto>> {
        val usersEntity = service.getUsers()
        val usersDto = usersEntity.map { service.convertToUserDto(it) }

        return ResponseEntity.ok(usersDto)
    }

    @PutMapping("/assigneUser/{id}/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun promoteUserToAdmin(@PathVariable id: Long, @PathVariable role: String, authentication: Authentication): ResponseEntity<UserDto> {
        logger.info { "Received a request from user id ${authentication.name} to promote user with id: $id to role: $role" }
        val userEntity = service.assigneeUserRole(id, role, authentication.name)
        val userDto = service.convertToUserDto(userEntity)

        return ResponseEntity.ok(userDto)
    }

    @PutMapping("/ban")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun banUser(@RequestBody request: BanRequest, authentication: Authentication): ResponseEntity<List<BanDto>> {
        logger.info { "Received a request from user id ${authentication.name} to ban users: ${request.userIds}" }

        val bannedBy = authentication.name.toLong()
        val reason = request.reason
        val duration = request.duration

        val bannedUsers = newArrayList<BanDto>()

        request.userIds.forEach { userId ->
            logger.info { "Received a request from user id ${authentication.name} to ban user with id: $userId for: $duration reason: $reason" }

            val ban = service.banUser(userId, bannedBy, reason, duration)
            val banDto = service.convertToBanDto(ban)
            bannedUsers.add(banDto)
        }

        return ResponseEntity.ok(bannedUsers)
    }

    @PatchMapping("/trustedStatus")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun setTrusted(@RequestBody request: UpdateTrustedUserStatusRequest, authentication: Authentication): ResponseEntity<List<UserDto>> {

        val isTrusted = request.trusted

        val users = newArrayList<UserDto>()

        request.usersIds.forEach { userId ->
            logger.info { "Received a request from user id ${authentication.name} to set user with id: $userId to trusted: $isTrusted" }

            val userEntity = service.setTrusted(userId, isTrusted)
            val userDto = service.convertToUserDto(userEntity)

            users.add(userDto)
        }

        return ResponseEntity.ok(users)
    }

    @GetMapping("/test")
    fun test(): ResponseEntity<String> {
        return ResponseEntity.ok("""
            {"text": "TestText"}
            """)

    }

}