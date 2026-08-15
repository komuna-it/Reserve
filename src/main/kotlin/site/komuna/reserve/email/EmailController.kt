package site.komuna.reserve.email

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import site.komuna.reserve.email.model.EmailRecipient
import site.komuna.reserve.email.model.EmailTemplateType
import site.komuna.reserve.user.UserService

@RestController
class EmailController(
    private val service: EmailService,
    private val userService: UserService,
) {

    @GetMapping("/email")
    fun sendEmail() {
        val map = mutableMapOf<String, Any>()
        val user = userService.findById(2)
        val recipient = EmailRecipient(user)
        service.sendEmailToUser(EmailTemplateType.USER_RESERVATION_CREATED, recipient, map)
    }
}