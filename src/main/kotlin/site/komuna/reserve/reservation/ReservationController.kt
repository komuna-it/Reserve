package site.komuna.reserve.reservation

import io.github.oshai.kotlinlogging.KotlinLogging
import org.hibernate.validator.internal.util.CollectionHelper.newArrayList
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import site.komuna.reserve.reservation.model.*
import site.komuna.reserve.user.UserService
import java.time.OffsetDateTime
import java.time.ZoneOffset

@RestController
@RequestMapping("/reservations")
class ReservationController(
    private val service: ReservationService,
    private val userService: UserService
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
        @RequestParam(required = false) privateReservation: Boolean?,
        @RequestParam(required = false) roomId: Long?,
        @RequestParam(required = false) startAtAfter: OffsetDateTime?,
        @RequestParam(required = false) startAtBefore: OffsetDateTime?,
        @RequestParam(required = false) status: List<ReservationStatus>?,
        @RequestParam(required = false) type: List<ReservationType>?,
        @PageableDefault(sort = ["startAt"], direction = Sort.Direction.DESC) pageable: Pageable
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
            status = status?.toMutableList() ?: mutableListOf(),
            type = type?.toMutableList() ?: mutableListOf(),
        )

        return service.getReservations(filter, pageable).map { ReservationDto(it) }
    }

    @PostMapping("")
    fun createReservation(
        @RequestBody request: CreateReservationRequest,
        authentication: Authentication
    ): ResponseEntity<ReservationDto> {
        val currentUserId = authentication.name.toLong()
        val currentUser = userService.findById(currentUserId)

        request.reservedAt = OffsetDateTime.now(ZoneOffset.UTC)
        request.startAt = request.startAt.withSecond(0).withNano(0)

        logger.trace { "Received a request from user $currentUserId to create a new reservation: $request" }

        val response = ReservationDto(service.createReservation(request, currentUser))

        return ResponseEntity.ok(response)
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun confirmReservation(@RequestBody request: ReservationStatusRequest, authentication: Authentication): ResponseEntity<List<ReservationDto>> {
        val confirmedBy = authentication.name.toLong()
        val reservations = newArrayList<ReservationDto>()

        request.reservationIds.forEach { reservationId ->
            logger.info { "Received a request from user id ${authentication.name} to confirm a reservation with id: $reservationId" }
            val response = service.confirmReservation(reservationId, confirmedBy)
            reservations.add(ReservationDto(response))
        }

        return ResponseEntity.ok(reservations)
    }

    @PostMapping("/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun rejectReservation(@RequestBody request: ReservationStatusRequest, authentication: Authentication): ResponseEntity<List<ReservationDto>> {
        val confirmedBy = authentication.name.toLong()
        val reservations = newArrayList<ReservationDto>()

        request.reservationIds.forEach { reservationId ->
            logger.info { "Received a request from user id ${authentication.name} to reject a reservation with id: $reservationId" }
            val response = service.rejectReservationRequest(reservationId, confirmedBy)
            reservations.add(ReservationDto(response))
        }

        return ResponseEntity.ok(reservations)
    }

    @PostMapping("/requestCancel")
    fun requestCancelReservation(@RequestBody request: ReservationStatusRequest, authentication: Authentication): ResponseEntity<List<ReservationDto>> {
        val cancelledBy = authentication.name.toLong()
        val reservations = newArrayList<ReservationDto>()

        request.reservationIds.forEach { reservationId ->
            logger.info { "Received a request from user id ${authentication.name} to request cancellation of reservation with id: $reservationId" }
            val response = service.requestCancelReservation(reservationId, cancelledBy)
            reservations.add(ReservationDto(response))
        }

        return ResponseEntity.ok(reservations)
    }

    @PostMapping("/confirmCancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun confirmCancelReservation(@RequestBody request: ReservationStatusRequest, authentication: Authentication): ResponseEntity<List<ReservationDto>> {
        val cancelledBy = authentication.name.toLong()
        val reservations = newArrayList<ReservationDto>()

        request.reservationIds.forEach { reservationId ->
            logger.info { "Received a request from user id ${authentication.name} to APPROVE request cancellation of reservation with id: $reservationId" }
            val response = service.confirmCancelReservation(reservationId, cancelledBy)
            reservations.add(ReservationDto(response))
        }

        return ResponseEntity.ok(reservations)
    }

    @PostMapping("/rejectCancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun rejectCancelReservation(@RequestBody request: ReservationStatusRequest, authentication: Authentication): ResponseEntity<List<ReservationDto>> {
        val cancelledBy = authentication.name.toLong()
        val reservations = newArrayList<ReservationDto>()

        request.reservationIds.forEach { reservationId ->
            logger.info { "Received a request from user id ${authentication.name} to REJECT request cancellation of reservation with id: $reservationId" }
            val response = service.rejectCancelReservation(reservationId, cancelledBy)
            reservations.add(ReservationDto(response))
        }

        return ResponseEntity.ok(reservations)
    }
}