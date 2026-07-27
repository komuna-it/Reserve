package site.komuna.reserve.reservation

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import site.komuna.reserve.reservation.model.CreateReservationRequest
import site.komuna.reserve.reservation.model.ReservationDto
import site.komuna.reserve.reservation.model.ReservationStatus
import site.komuna.reserve.reservation.model.ReservationType
import site.komuna.reserve.reservation.model.SearchReservationsFilter
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

    @GetMapping("")
    fun searchReservations(
        @RequestParam(required = false) reservationId: Long?,
        @RequestParam(required = false) userId: Long?,
        @RequestParam(required = false) reservedBy: Long?,
        @RequestParam(required = false) organizationsId: List<Long>?,
        @RequestParam(defaultValue = "false") future: Boolean,
        @RequestParam(defaultValue = "false") privateReservation: Boolean,
        @RequestParam(required = false) roomId: Long?,
        @RequestParam(required = false) startAtAfter: OffsetDateTime?,
        @RequestParam(required = false) startAtBefore: OffsetDateTime?,
        @RequestParam(required = false) status: List<String>?,
        @RequestParam(required = false) type: List<String>?,
        pageable: Pageable
    ): Page<ReservationDto> {

        val filter = SearchReservationsFilter(
            reservationId = reservationId,
            userId = userId,
            reservedBy = reservedBy,
            organizationsId = organizationsId?.toMutableList() ?: mutableListOf(),
            future = future,
            private = privateReservation,
            roomId = roomId,
            startAtAfter = startAtAfter,
            startAtBefore = startAtBefore,
            status = status?.map { ReservationStatus.valueOf(it) }?.toMutableList() ?: mutableListOf(),
            type = type?.map { ReservationType.valueOf(it) }?.toMutableList() ?: mutableListOf(),
        )

        val response = service.getReservations(filter, pageable).map { ReservationDto(it) }

        return response
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

        logger.info { "Received a request from user id ${authentication.name} to confirm a reservation with id: $reservationId" }

        val response = service.confirmReservation(reservationId, confirmedBy)

        return ResponseEntity.ok(ReservationDto(response))
    }

    @PostMapping("/reject/{reservationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun rejectReservation(@PathVariable reservationId: Long, authentication: Authentication): ResponseEntity<ReservationDto> {
        val confirmedBy = authentication.name.toLong()

        logger.info { "Received a request from user id ${authentication.name} to reject a reservation with id: $reservationId" }

        val response = service.rejectReservationRequest(reservationId, confirmedBy)

        return ResponseEntity.ok(ReservationDto(response))
    }

    @PostMapping("/requestCancel/{reservationId}")
    fun requestCancelReservation(@PathVariable reservationId: Long, authentication: Authentication): ResponseEntity<ReservationDto> {
        val cancelledBy = authentication.name.toLong()

        logger.info { "Received a request from user id ${authentication.name} to request cancellation of reservation with id: $reservationId" }
        val response = service.requestCancelReservation(reservationId, cancelledBy)

        return ResponseEntity.ok(ReservationDto(response))
    }

    @PostMapping("/confirmCancel/{reservationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun confirmCancelReservation(@PathVariable reservationId: Long, authentication: Authentication): ResponseEntity<ReservationDto> {
        val cancelledBy = authentication.name.toLong()

        logger.info { "Received a request from user id ${authentication.name} to APPROVE request cancellation of reservation with id: $reservationId" }
        val response = service.confirmCancelReservation(reservationId, cancelledBy)

        return ResponseEntity.ok(ReservationDto(response))
    }

    @PostMapping("/rejectCancel/{reservationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun rejectCancelReservation(@PathVariable reservationId: Long, authentication: Authentication): ResponseEntity<ReservationDto> {
        val cancelledBy = authentication.name.toLong()

        logger.info { "Received a request from user id ${authentication.name} to REJECT request cancellation of reservation with id: $reservationId" }
        val response = service.rejectCancelReservation(reservationId, cancelledBy)

        return ResponseEntity.ok(ReservationDto(response))
    }
}