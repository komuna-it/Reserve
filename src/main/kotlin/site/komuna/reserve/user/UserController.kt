package site.komuna.reserve.user

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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

    @GetMapping("/me")
    fun me(authentication: Authentication): Any {
        return authentication.name
    }
}