package site.komuna.reserve.reservation

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.komuna.reserve.reservation.model.CreateReservationRequest
import site.komuna.reserve.reservation.model.ReservationDto
import java.time.OffsetDateTime
import java.time.ZoneOffset

@RestController
@RequestMapping("/reservations")
class ReservationController(
    private val service: ReservationService
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @PostMapping("")
    fun createReservation(@RequestBody request: CreateReservationRequest, authentication: Authentication): ResponseEntity<ReservationDto> {

        request.reservedByUserId = authentication.name.toLong()
        request.reservedAt = OffsetDateTime.now(ZoneOffset.UTC)

        request.startAt = request.startAt.withSecond(0).withNano(0)

        logger.trace { "Received a request from user ${request.reservedByUserId} to create a new reservation: $request" }

        val response = ReservationDto(service.createReservation(request))

        return ResponseEntity.ok(response)
    }

    @PostMapping("/confirm/{reservationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun confirmReservation(@PathVariable reservationId: Long, authentication: Authentication): ResponseEntity<ReservationDto> {
        val confirmedBy = authentication.name.toLong()

        logger.info { "Received a request from user id ${authentication.name} to confirm reservation with id: $reservationId" }

        val response = service.confirmReservation(reservationId, confirmedBy)

        return ResponseEntity.ok(ReservationDto(response))
    }
}