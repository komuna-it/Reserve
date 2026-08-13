package site.komuna.reserve.user

import io.github.oshai.kotlinlogging.KotlinLogging
import org.hibernate.validator.internal.util.CollectionHelper.newArrayList
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
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
import site.komuna.reserve.user.ban.model.UnBanRequest
import site.komuna.reserve.user.model.UpdateTrustedUserStatusRequest
import site.komuna.reserve.user.model.UserDto
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.PostMapping
import site.komuna.reserve.user.model.ForgotPasswordRequest
import site.komuna.reserve.user.model.UpdatePasswordRequest

@RestController
@RequestMapping("/users")
class UserController(
    private val service: UserService
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }


    @GetMapping("/all")
    fun getUsers(
        @PageableDefault(size = 10, page = 0, sort = ["id"], direction = Sort.Direction.ASC) pageable: Pageable
    ): ResponseEntity<org.springframework.data.domain.Page<UserDto>> {
        return ResponseEntity.ok(service.getUsers(pageable))
    }


    @PutMapping("/assigneUser/{id}/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun promoteUserToAdmin(@PathVariable id: Long, @PathVariable role: String, authentication: Authentication): ResponseEntity<UserDto> {
        logger.info { "Received a request from user id ${authentication.name} to promote user with id: $id to role: $role" }
        val userEntity = service.assigneeUserRole(id, role, authentication.name)
        val userDto = service.convertToUserDto(userEntity)

        return ResponseEntity.ok(userDto)
    }

    @PutMapping("/updatePassword")
    fun updatePassword(@RequestBody request: UpdatePasswordRequest, authentication: Authentication): ResponseEntity<UserDto> {
        val userId = authentication.name.toLong()

        val userEntity = service.updatePassword(userId, request.currentPassword, request.newPassword)
        val userDto = service.convertToUserDto(userEntity)
        return ResponseEntity.ok(userDto)
    }

    @PostMapping("/forgotPassword")
    fun forgotPassword(@RequestBody request: ForgotPasswordRequest): ResponseEntity<Void> {
        service.forgotPassword(request.email)

        return ResponseEntity.ok().build()
    }

    @PutMapping("/ban")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun banUser(@RequestBody request: BanRequest, authentication: Authentication): ResponseEntity<List<BanDto>> {
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

    @PutMapping("/unban")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun unbanUser(@RequestBody request: UnBanRequest, authentication: Authentication): ResponseEntity<List<UserDto>> {

        val unbannedUsers = newArrayList<UserDto>()

        request.userIds.forEach { userId ->
            logger.info { "Received a request from user id ${authentication.name} to unban user with id: $userId" }
            val userEntity = service.unbanUser(userId)
            val userDto = service.convertToUserDto(userEntity)
            unbannedUsers.add(userDto)
        }
        return ResponseEntity.ok(unbannedUsers)
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

    @PatchMapping("/preferredLanguage/{language}")
    fun setPreferredLanguage(@PathVariable language: String, authentication: Authentication): ResponseEntity<UserDto> {
        val userId = authentication.name.toLong()

        val response = service.assigneePreferredLanguage(userId, language)
        val userDto = service.convertToUserDto(response)
        return ResponseEntity.ok(userDto)
    }

    @GetMapping("/test")
    fun test(): ResponseEntity<String> {
        return ResponseEntity.ok("""
            {"text": "TestText"}
            """)

    }

}