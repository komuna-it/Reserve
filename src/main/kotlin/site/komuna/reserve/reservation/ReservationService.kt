package site.komuna.reserve.reservation

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import site.komuna.reserve.common.exception.CannotPerformThatActionException
import site.komuna.reserve.common.exception.ReservationNotFoundException
import site.komuna.reserve.organization.OrganizationService
import site.komuna.reserve.reservation.cancel.CancelReservationService
import site.komuna.reserve.reservation.confirm.ConfirmReservationService
import site.komuna.reserve.reservation.model.CreateReservationRequest
import site.komuna.reserve.reservation.model.ReservationEntity
import site.komuna.reserve.reservation.model.ReservationStatus
import site.komuna.reserve.reservation.validation.CreateReservationValidation
import site.komuna.reserve.room.RoomService
import site.komuna.reserve.user.UserService
import site.komuna.reserve.user.model.UserEntity
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class ReservationService(
    private val repository: ReservationRepository,
    private val confirmReservationService: ConfirmReservationService,
    private val cancelReservationService: CancelReservationService,
    private val organizationService: OrganizationService,
    private val roomService: RoomService,
    private val userService: UserService,
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun findById(id: Long): ReservationEntity {
        return repository.findById(id).orElseThrow { ReservationNotFoundException(id) }
    }

    fun createReservation(request: CreateReservationRequest): ReservationEntity {
        prepareRequest(request)
        validate(request)

        val response = repository.save(ReservationEntity(request))

        confirmReservationIfTrusted(response)

        // TODO: Emit event

        return response
    }

    // Confirm reservation
    /**
     * Reservation could be automatically confirmed if a user or organization is trusted
     */
    fun confirmReservationIfTrusted(reservation: ReservationEntity) {
        val user = reservation.reservedBy
        val organization = reservation.organization

        if (user.trusted) {
            logger.trace { "Automatically confirming reservation ${reservation.id} because user ${user.id} is trusted" }
            confirmReservationBySystem(reservation)
        }

        if (organization != null && organization.trusted) {
            logger.trace { "Automatically confirming reservation ${reservation.id} because organization ${organization.id} is trusted" }
            confirmReservationBySystem(reservation)
        }
    }

    fun confirmReservationBySystem(reservation: ReservationEntity): ReservationEntity {
        val systemUser = userService.getSystemUser()
        return confirmReservation(reservation, systemUser)
    }

    fun confirmReservation(reservation: ReservationEntity, approvedBy: UserEntity): ReservationEntity{
        if (reservation.status != ReservationStatus.CREATED) {
            throw CannotPerformThatActionException("Reservation is not in CREATED status")
        }

        // Save details
        confirmReservationService.saveConfirmReservationDetails(reservation, approvedBy)

        return changeStatus(reservation, ReservationStatus.CONFIRMED)
    }

    fun confirmReservation(reservationId: Long, approvedBy: Long): ReservationEntity {
        val user = userService.findById(approvedBy)
        val reservation = findById(reservationId)

        return confirmReservation(reservation, user)
    }

    // Cancel reservation
    fun requestCancelReservation(reservationId: Long, cancelledBy: Long): ReservationEntity {
        val reservation = findById(reservationId)
        val cancelledByUser = userService.findById(cancelledBy)

        return requestCancelReservation(reservation, cancelledByUser)
    }

    fun requestCancelReservation(reservation: ReservationEntity, cancelledByUser: UserEntity): ReservationEntity {
        val startAt = reservation.startAt
        val canceledAt = OffsetDateTime.now(ZoneOffset.UTC)

        if(reservation.status == ReservationStatus.REQUESTED_CANCELLATION) {
            throw CannotPerformThatActionException("Reservation is already in REQUESTED_CANCELLATION status")
        }

        if(reservation.status == ReservationStatus.CANCELLED) {
            throw CannotPerformThatActionException("Reservation is already in CANCELLED status")
        }

        if(reservation.status == ReservationStatus.REJECTED_CANCELLATION) {
            throw CannotPerformThatActionException("Reservation is already in REJECTED_CANCELLATION status")
        }

        val time = Duration.between(canceledAt, startAt)

        // Allow user to cancel a reservation within 24 hours
        if(time.toHours() > 24) {
            return cancelReservationBySystem(reservation, cancelledByUser, canceledAt)
        }

        // Save cancellation request
        return saveCancellationRequest(reservation, cancelledByUser, canceledAt)
    }

    fun cancelReservationBySystem(reservation: ReservationEntity, canceledBy: UserEntity, canceledAt: OffsetDateTime): ReservationEntity {
        val systemUser = userService.getSystemUser()
        cancelReservationService.saveCancelReservationDetails(reservation,
            canceledBy,
            canceledAt,
            systemUser,
            canceledAt)

        return changeStatus(reservation, ReservationStatus.CANCELLED)
    }

    fun saveCancellationRequest(reservation: ReservationEntity, cancelledBy: UserEntity, canceledAt: OffsetDateTime): ReservationEntity {
        cancelReservationService.saveCancelReservationDetails(reservation,
            cancelledBy,
            canceledAt, null, null)

        return changeStatus(reservation, ReservationStatus.REQUESTED_CANCELLATION)
    }

    // Confirm cancel reservation
    fun confirmCancelReservation(reservationId: Long, approvedBy: Long): ReservationEntity {
        val reservation = findById(reservationId)
        val approvedByUser = userService.findById(approvedBy)

        return confirmCancelReservation(reservation, approvedByUser)
    }

    fun confirmCancelReservation(reservation: ReservationEntity, approvedBy: UserEntity): ReservationEntity {
        if(reservation.status != ReservationStatus.REQUESTED_CANCELLATION) {
            throw CannotPerformThatActionException("Reservation is not in REQUESTED_CANCELLATION status")
        }

        cancelReservationService.updateCancelReservationDetails(reservation, approvedBy)
        return changeStatus(reservation, ReservationStatus.CANCELLED)
    }

    // Reject cancel reservation
    fun rejectCancelReservation(reservationId: Long, approvedBy: Long): ReservationEntity {
        val reservation = findById(reservationId)
        val approvedByUser = userService.findById(approvedBy)

        return rejectCancelReservation(reservation, approvedByUser)
    }

    fun rejectCancelReservation(reservation: ReservationEntity, approvedBy: UserEntity): ReservationEntity {
        if(reservation.status != ReservationStatus.REQUESTED_CANCELLATION) {
            throw CannotPerformThatActionException("Reservation is not in REQUESTED_CANCELLATION status")
        }

        cancelReservationService.updateCancelReservationDetails(reservation, approvedBy)
        return changeStatus(reservation, ReservationStatus.REJECTED_CANCELLATION)
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

    // VALIDATION
    fun validate(request: CreateReservationRequest) {

        val validator = CreateReservationValidation(organizationService, repository)
        validator.validate(request)
    }


    fun changeStatus(reservation: ReservationEntity, status: ReservationStatus): ReservationEntity {
        logger.trace { "Changing reservation ${reservation.id} status from ${reservation.status} to $status" }
        reservation.status = status
        return repository.save(reservation)
    }
}