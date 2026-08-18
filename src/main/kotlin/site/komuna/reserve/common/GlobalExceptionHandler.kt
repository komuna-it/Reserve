package site.komuna.reserve.common

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.exception.MissingDataException
import site.komuna.reserve.common.httpError.exception.ValidationException
import java.time.OffsetDateTime
import java.time.ZoneOffset

@RestControllerAdvice
class GlobalExceptionHandler {
    /**
     * ReserveErrorResponse is a standardized error response format.
     */
    data class ReserveErrorResponse(
        val timestamp: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
        val type: String,
        val body: Any?
    )

    /**
     * Handler catches HttpReserveException and converts them to response
     * All exceptions supposed to be handled that way
     */
    @ExceptionHandler(HttpReserveException::class)
    fun handleHttpReserveException(ex: HttpReserveException): ResponseEntity<ReserveErrorResponse> {
        return ResponseEntity
            .status(ex.httpStatus)
            .body(ReserveErrorResponse(type = ex.type.name, body = ex.body))
    }

    /**
     * Handler catches validation exceptions created by jakarta validation annotations.
     * And convert them to ReserveErrorResponse.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException
    ): ResponseEntity<ReserveErrorResponse> {

        val errors = ex.bindingResult.fieldErrors
            .groupBy(
                keySelector = { it.field },
                valueTransform = { it.defaultMessage ?: "Invalid value" }
            )

        return handleHttpReserveException(
            ValidationException(errors)
        )
    }

    /**
     * Handle missing fields in a request body.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ReserveErrorResponse> {
        val cause = ex.cause

        if (cause is MismatchedInputException) {
            val field = cause.path.lastOrNull()?.fieldName ?: throw ex

            return handleHttpReserveException(MissingDataException(field))
        }

        throw ex
    }
}