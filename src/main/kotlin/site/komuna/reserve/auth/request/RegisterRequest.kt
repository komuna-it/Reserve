package site.komuna.reserve.auth.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

class RegisterRequest(

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email is not valid")
    val email: String,

    @field:NotBlank(message = "Nick is required")
    val name: String,

    @field:NotBlank(message = "Password is required")
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "Password must contain at least one uppercase letter, one lowercase letter and one digit"
    )
    val password: String,
) {
}