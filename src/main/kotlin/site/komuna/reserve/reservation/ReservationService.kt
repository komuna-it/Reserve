package site.komuna.reserve.reservation

import org.springframework.stereotype.Service
import site.komuna.reserve.common.exception.CannotPerformThatActionException
import site.komuna.reserve.organization.OrganizationService
import site.komuna.reserve.reservation.model.CreateReservationRequest
import site.komuna.reserve.reservation.model.ReservationEntity
import site.komuna.reserve.room.RoomService
import site.komuna.reserve.user.UserService
import java.time.OffsetDateTime

@Service
class ReservationService(
    private val repository: ReservationRepository,
    private val organizationService: OrganizationService,
    private val roomService: RoomService,
    private val userService: UserService,
) {

    fun createReservation(request: CreateReservationRequest): ReservationEntity {
        prepareRequest(request)

        validateOrganizationMembership(request)
        isStartAtInFuture(request)
        isRoomAvailable(request)
        isReservationInAllowedRange(request)
        isDurationValid(request)

        val response = repository.save(ReservationEntity(request))

        // TODO: Emit event

        return response
    }

    // Prepare request
    fun prepareRequest(request: CreateReservationRequest) {
        request.reservedByUser = userService.findById(request.reservedByUserId!!)
        request.room = roomService.getRoom(request.roomId)
        request.endAt = request.startAt.plusMinutes(request.duration.toMinutes())

        if(request.organizationId != null) {
            request.organization = organizationService.getOrganization(request.organizationId!!)
        }

    }

    // VALIDATIONS

    fun validateOrganizationMembership(request: CreateReservationRequest): Boolean {
        if(request.organization == null && request.organizationId == null) {
            return true
        }

        val organization = request.organization!!
        val user = request.reservedByUser!!

        if(!organizationService.isMember(user, organization)) {
            throw CannotPerformThatActionException("User with ${user.id} is not a member of the organization ${organization.name}")
        }

        return true
    }

    fun isRoomAvailable(request: CreateReservationRequest): Boolean {
        val reservations = repository.findOverlappingReservations(request.room!!.id!!, request.startAt, request.endAt!!)

        if(reservations.isNotEmpty()) {
            throw CannotPerformThatActionException("Room ${request.room!!.name} is not available at ${request.startAt} - ${request.endAt}")
        }

        return true
    }

    fun isStartAtInFuture(reservation: CreateReservationRequest): Boolean {
        val startAt = reservation.startAt
        val now = OffsetDateTime.now()

        if (startAt.isBefore(now)) {
            throw CannotPerformThatActionException("Can not create reservation in the past")
        }

        return true
    }

    fun isReservationInAllowedRange(reservation: CreateReservationRequest): Boolean {
        val startAt = reservation.startAt
        val endAt = reservation.startAt.plusMinutes(reservation.duration.toMinutes())

        // Make sure that reservation is within allowed hours
        val startAtHour = startAt.hour
        val endAtHour = endAt.hour

        val serviceStartHours = 10
        val serviceEndHours = 22

        if (startAtHour < serviceStartHours || endAtHour > serviceEndHours) {
            throw CannotPerformThatActionException("Reservation is outside of allowed hours")
        }

        // Make sure that reservation is within allowed minutes
        val startAtMinute = startAt.minute
        val endAtMinute = endAt.minute

        if (startAtMinute % 30 != 0) {
            throw CannotPerformThatActionException("Reservation time is not on a 30-minute interval")
        }

        return true
    }

    fun isDurationValid(request: CreateReservationRequest): Boolean {
        val duration = request.duration

        if (duration.toMinutes() % 60 != 0L) {
            throw CannotPerformThatActionException("Duration must be a multiple of 60 minutes")
        }

        return true
    }
}