package site.komuna.reserve.reservation.validation

import site.komuna.reserve.common.exception.CannotPerformThatActionException
import site.komuna.reserve.organization.OrganizationService
import site.komuna.reserve.organization.organizationMember.OrganizationMemberService
import site.komuna.reserve.reservation.ReservationRepository
import site.komuna.reserve.reservation.model.CreateReservationRequest
import site.komuna.reserve.user.Role
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime

class CreateReservationValidation(
    private val organizationService: OrganizationService,
    private val reservationRepository: ReservationRepository,
    private val organizationMemberService: OrganizationMemberService
) {

    fun validate(request: CreateReservationRequest, currentUser: UserEntity): Boolean {
        isStartAtInFuture(request)
        isRoomAvailable(request)
        isReservationInAllowedRange(request)
        isDurationValid(request)

        if (request.organization != null) {
            val organization = request.organization!!
            val targetUser = request.reservedByUser!!

            if (currentUser.role != Role.ADMIN) {
                val isMember = organizationMemberService.isMember(targetUser.id!!, organization.id!!)
                val isOwner = organizationService.isOwner(targetUser, organization)

                if (!isMember && !isOwner) {
                    throw CannotPerformThatActionException(
                        "User with id ${targetUser.id} is not an owner/member of organization ${organization.name}"
                    )
                }
            }
        }

        return true
    }

    fun isRoomAvailable(request: CreateReservationRequest): Boolean {
        val reservations = reservationRepository.findOverlappingReservations(
            request.room!!.id!!,
            request.startAt,
            request.endAt!!
        )

        if (reservations.isNotEmpty()) {
            throw CannotPerformThatActionException(
                "Room ${request.room!!.name} is not available at ${request.startAt} - ${request.endAt}"
            )
        }

        return true
    }

    fun isStartAtInFuture(reservation: CreateReservationRequest): Boolean {
        if (reservation.startAt.isBefore(OffsetDateTime.now())) {
            throw CannotPerformThatActionException("Can not create reservation in the past")
        }
        return true
    }

    fun isReservationInAllowedRange(reservation: CreateReservationRequest): Boolean {
        val startAt = reservation.startAt
        val endAt = reservation.startAt.plusMinutes(reservation.duration.toMinutes())

        val serviceStartHours = 10
        val serviceEndHours = 22

        if (startAt.hour < serviceStartHours || endAt.hour > serviceEndHours) {
            throw CannotPerformThatActionException("Reservation is outside of allowed hours")
        }

        if (startAt.minute % 30 != 0) {
            throw CannotPerformThatActionException("Reservation time is not on a 30-minute interval")
        }

        return true
    }

    fun isDurationValid(request: CreateReservationRequest): Boolean {
        if (request.duration.toMinutes() % 60 != 0L) {
            throw CannotPerformThatActionException("Duration must be a multiple of 60 minutes")
        }
        return true
    }
}