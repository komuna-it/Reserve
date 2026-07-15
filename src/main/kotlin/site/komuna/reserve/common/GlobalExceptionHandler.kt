package site.komuna.reserve.common

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import site.komuna.reserve.common.exception.ReserveException
import java.time.OffsetDateTime
import java.time.ZoneOffset

@RestControllerAdvice
class GlobalExceptionHandler {

    data class ErrorResponse(
        val timestamp: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
        val status: Int,
        val error: String
    )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(ex: MethodArgumentNotValidException): Map<String, String> {

        return ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Invalid value")
        }
    }

    @ExceptionHandler(ReserveException::class)
    fun handleReserveException(ex: ReserveException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(ex.httpStatus)
            .body(
                ErrorResponse(
                    status = ex.httpStatus.value(),
                    error = ex.message
                )
            )
    }
}