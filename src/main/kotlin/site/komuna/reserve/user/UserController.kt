package site.komuna.reserve.user

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.komuna.reserve.user.ban.model.BanDto
import site.komuna.reserve.user.ban.model.BanRequest
import site.komuna.reserve.user.model.UserDto

@RestController
@RequestMapping("/users")
class UserController(
    private val service: UserService
) {

    @PutMapping("/assigneUser/{id}/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun promoteUserToAdmin(@PathVariable id: Long, @PathVariable role: String, authentication: Authentication): ResponseEntity<UserDto> {
        val user = UserDto(service.assigneeUserRole(id, role, authentication.name))

        return ResponseEntity.ok(user)
    }

    @PutMapping("/ban")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun banUser(@RequestBody request: BanRequest, authentication: Authentication): ResponseEntity<BanDto> {
        val userId = request.userId
        val bannedBy = authentication.name.toLong()
        val reason = request.reason
        val duration = request.duration

        val ban = service.banUser(userId, bannedBy, reason, duration)
        return ResponseEntity.ok(BanDto(ban))
    }

    @GetMapping("/test")
    fun test(): ResponseEntity<String> {
        return ResponseEntity.ok("Test")
    }

}