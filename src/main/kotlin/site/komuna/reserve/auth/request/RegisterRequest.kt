package site.komuna.reserve.auth.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

class RegisterRequest(

    @NotBlank(message = "Email is required")
    @Email(message = "Email is not valid")
    val email: String,

    @NotBlank(message = "Nick is required")
    val name: String,

    @NotBlank(message = "Password is required")
    val password: String,
) {
}