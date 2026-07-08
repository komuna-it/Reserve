package site.komuna.reserve.auth.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

class LoginRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email is not valid")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String,
) {

}