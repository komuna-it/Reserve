package site.komuna.reserve.reservation

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import org.hibernate.Hibernate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.komuna.reserve.common.PageResponse
import site.komuna.reserve.common.exception.CannotPerformThatActionException
import site.komuna.reserve.common.exception.ReservationNotFoundException
import site.komuna.reserve.common.toPageResponse
import site.komuna.reserve.email.EmailService
import site.komuna.reserve.email.model.EmailRecipient
import site.komuna.reserve.email.model.EmailTemplateType
import site.komuna.reserve.organization.OrganizationService
import site.komuna.reserve.organization.model.OrganizationEntity
import site.komuna.reserve.organization.organizationMember.OrganizationMemberService
import site.komuna.reserve.reservation.cancel.CancelReservationService
import site.komuna.reserve.reservation.confirm.ConfirmReservationService
import site.komuna.reserve.reservation.model.*
import site.komuna.reserve.reservation.validation.CreateReservationValidation
import site.komuna.reserve.room.RoomService
import site.komuna.reserve.room.model.RoomEntity
import site.komuna.reserve.room.pricing.PricingService
import site.komuna.reserve.settings.SettingsService
import site.komuna.reserve.settings.model.SettingsKey
import site.komuna.reserve.sse.ReserveEvents
import site.komuna.reserve.sse.SseService
import site.komuna.reserve.user.Role
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
    private val sseService: SseService,
    private val settings: SettingsService,
    private val emailService: EmailService,
    private val pricingService: PricingService,
    private val organizationMemberService: OrganizationMemberService,
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun getReservations(filter: SearchReservationsFilter, pageable: Pageable): Page<ReservationEntity> {
        prepareSearch(filter)

        val spec = specification(filter)

        return repository.findAll(spec, pageable)
    }

    fun getTomorrowReservations(): List<ReservationEntity> {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val tomorrow = now.plusDays(1)
        val startAt = tomorrow.withHour(0).withMinute(0).withSecond(0).withNano(0)
        val endAt = tomorrow.withHour(23).withMinute(59).withSecond(59).withNano(999999999)
        return repository.findByStartAtBetween(startAt, endAt)
    }

    fun specification(filter: SearchReservationsFilter) =
        Specification<ReservationEntity> { root, _, cb ->
            val predicates = mutableListOf<Predicate>()

            filter.reservationId?.let {
                predicates += cb.equal(root.get<Long>("id"), it)
            }

            when (filter.private) {
                true -> {
                    predicates += cb.isNull(root.get<Any>("organization"))

                    val targetUserId = filter.userId ?: filter.reservedBy
                    targetUserId?.let { uid ->
                        predicates += cb.equal(root.get<UserEntity>("reservedBy").get<Long>("id"), uid)
                    }
                }
                false -> {
                    val organizationJoin = root.join<ReservationEntity, OrganizationEntity>("organization", JoinType.INNER)
                    predicates += cb.isNotNull(root.get<Any>("organization"))

                    val orgIds = mutableSetOf<Long>()
                    orgIds.addAll(filter.organizationsId)
                    filter.reservedBy?.let { orgIds.add(it) }

                    if (orgIds.isNotEmpty()) {
                        predicates += organizationJoin.get<Long>("id").`in`(orgIds)
                    }

                    filter.userId?.let { uid ->
                        predicates += cb.equal(root.get<UserEntity>("reservedBy").get<Long>("id"), uid)
                    }
                }
                null -> {
                    if (filter.userId != null) {
                        val userIsCreator = cb.equal(root.get<UserEntity>("reservedBy").get<Long>("id"), filter.userId)
                        if (filter.organizationsId.isNotEmpty()) {
                            val organizationJoin = root.join<ReservationEntity, OrganizationEntity>("organization", JoinType.LEFT)
                            val inUserOrganizations = organizationJoin.get<Long>("id").`in`(filter.organizationsId)
                            predicates += cb.or(userIsCreator, inUserOrganizations)
                        } else {
                            predicates += userIsCreator
                        }
                    } else if (filter.organizationsId.isNotEmpty()) {
                        val organizationJoin = root.join<ReservationEntity, OrganizationEntity>("organization", JoinType.INNER)
                        predicates += organizationJoin.get<Long>("id").`in`(filter.organizationsId)
                    } else filter.reservedBy?.let { rId ->
                        val userIsCreator = cb.equal(root.get<UserEntity>("reservedBy").get<Long>("id"), rId)
                        val organizationJoin = root.join<ReservationEntity, OrganizationEntity>("organization", JoinType.LEFT)
                        val isOrg = organizationJoin.get<Long>("id").`in`(listOf(rId))
                        predicates += cb.or(userIsCreator, isOrg)
                    }
                }
            }

            if (filter.future) {
                predicates += cb.greaterThanOrEqualTo(
                    root.get("startAt"),
                    OffsetDateTime.now(ZoneOffset.UTC)
                )
            }

            filter.roomId?.let { rid ->
                val roomJoin = root.join<ReservationEntity, RoomEntity>("room")
                predicates += cb.equal(roomJoin.get<Long>("id"), rid)
            }

            filter.startAtAfter?.let {
                predicates += cb.greaterThanOrEqualTo(root.get<OffsetDateTime>("startAt"), it)
            }

            filter.startAtBefore?.let {
                predicates += cb.lessThanOrEqualTo(root.get<OffsetDateTime>("startAt"), it)
            }

            filter.status.takeIf { it.isNotEmpty() }?.let {
                predicates += root.get<ReservationStatus>("status").`in`(it)
            }

            cb.and(*predicates.toTypedArray())
        }

    fun findById(id: Long): ReservationEntity {
        return repository.findById(id).orElseThrow { ReservationNotFoundException(id) }
    }

    fun createReservation(request: CreateReservationRequest, currentUser: UserEntity): ReservationEntity {
        prepareCreateRequest(request, currentUser)
        validate(request, currentUser)

        val response = repository.save(ReservationEntity(request))

        if (currentUser.role == Role.ADMIN) {
            confirmReservationBySystem(response)
        } else {
            confirmReservationIfTrusted(response)
        }

        sendCreatedReservationEmail(response)
        sseService.broadcast(ReserveEvents.RESERVATION_CREATED, ReservationDto(response))

        return response
    }
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
        if (reservation.status != ReservationStatus.CREATED && (approvedBy.role== Role.MANAGER || approvedBy.role== Role.ADMIN)){
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

    fun rejectReservationRequest(reservation: ReservationEntity, rejectedBy: UserEntity): ReservationEntity {
        if (reservation.status == ReservationStatus.REJECTED || reservation.status == ReservationStatus.CANCELLED) {
            throw CannotPerformThatActionException("Reservation is already in ${reservation.status} status")
        }

        confirmReservationService.saveRejectReservationDetails(reservation, rejectedBy)

        val response = changeStatus(reservation, ReservationStatus.REJECTED)
        sseService.broadcast(ReserveEvents.RESERVATION_REJECTED, ReservationDto(response))

        return response
    }

    fun rejectReservationRequest(reservationId: Long, rejectedBy: Long): ReservationEntity {
        val user = userService.findById(rejectedBy)
        val reservation = findById(reservationId)

        return rejectReservationRequest(reservation, user)
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

        // Allow user to cancel a reservation within X hours without admin approval
        val allowedHour = settings.getIntValue(SettingsKey.RESERVATION_CANCELLATION_WITHOUT_APPROVAL_HOURS)

        if(time.toHours() > allowedHour) {
            val response = cancelReservationBySystem(reservation, cancelledByUser, canceledAt)
            sseService.broadcast(ReserveEvents.RESERVATION_CANCELED, ReservationDto(response))
            return response
        }

        // Save cancellation request
        val response = saveCancellationRequest(reservation, cancelledByUser, canceledAt)
        sseService.broadcast(ReserveEvents.RESERVATION_CANCEL_REQUESTED, ReservationDto(response))
        return response
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
        val response = changeStatus(reservation, ReservationStatus.CANCELLED)
        sseService.broadcast(ReserveEvents.RESERVATION_CANCELED, ReservationDto(response))
        return response
    }

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
        val response = changeStatus(reservation, ReservationStatus.REJECTED_CANCELLATION)
        sseService.broadcast(ReserveEvents.RESERVATION_CANCEL_REJECTED, ReservationDto(response))
        return response
    }

    fun prepareCreateRequest(request: CreateReservationRequest, currentUser: UserEntity) {
        val targetUserId: Long = if (currentUser.role == Role.ADMIN && request.reservedByUserId != null) {
            request.reservedByUserId!!
        } else {
            currentUser.id!!
        }

        request.reservedByUserId = targetUserId
        request.reservedByUser = userService.findById(targetUserId)
        request.room = roomService.getRoom(request.roomId)
        request.endAt = request.startAt.plusMinutes(request.duration.toMinutes())

        request.organization = request.organizationId?.let { organizationService.getOrganization(it) }
    }

    fun setPaid(reservationId: Long, paid: Boolean, changedBy: Long): ReservationEntity {
        val reservation = findById(reservationId)
        return setPaid(reservation, paid, changedBy)
    }

    fun setPaid(reservation: ReservationEntity, paid: Boolean, changedBy: Long): ReservationEntity {
        reservation.paid = paid
        return repository.save(reservation)
    }
    // VALIDATION
    fun validate(request: CreateReservationRequest, currentUser: UserEntity) {
        val validator = CreateReservationValidation(organizationService, repository, organizationMemberService, settings)
        validator.validate(request, currentUser)
    }

    fun changeStatus(reservation: ReservationEntity, status: ReservationStatus): ReservationEntity {
        logger.trace { "Changing reservation ${reservation.id} status from ${reservation.status} to $status" }
        reservation.status = status
        return repository.save(reservation)
    }

    private fun prepareSearch(filter: SearchReservationsFilter): SearchReservationsFilter {
        if (filter.private == true) {
            filter.organizationsId.clear()
        }
        else if (filter.reservedBy != null && filter.organizationsId.isEmpty()) {
            filter.organizationsId.add(filter.reservedBy!!)
        }

        return filter
    }

    fun getReservationDto(reservation: ReservationEntity): ReservationDto {
        val price = pricingService.getPrice(reservation)
        return ReservationDto(reservation, price)
    }

    // EMAILS
     fun sendEmail(privateTemplate: EmailTemplateType, organizationTemplate: EmailTemplateType, reservation: ReservationEntity) {
        val user = reservation.reservedBy
        val startAt = reservation.startAt
        val endAt = reservation.endAt
        val duration = Duration.between(startAt, endAt)

        val model = mutableMapOf<String, Any>()

        model["roomName"] = reservation.room.name
        model["duration"] = duration.toHoursPart()
        model["startAt"] = startAt.toLocalTime()
        model["endAt"] = endAt.toLocalTime()

        // Handle private reservation
        if(reservation.organization == null) {
            val recipient = EmailRecipient(user)

            emailService.sendEmailToUser(privateTemplate, recipient, model)
        }
        // Handle organization reservation
        else {
            val recipients = mutableListOf<EmailRecipient>()

            val organization = reservation.organization!!
            val members = organizationMemberService.getAllOrganizationUsers(organization.id!!)

            members.forEach {
                recipients.add(EmailRecipient(it))
            }

            model["organizationName"] = organization.name

            emailService.sendEmailToUsers(organizationTemplate, recipients, model)
        }
    }

    private fun sendCreatedReservationEmail(reservation: ReservationEntity) {
        sendEmail(EmailTemplateType.RESERVATION_CREATED_PRIVATE, EmailTemplateType.RESERVATION_CREATED_ORGANIZATION, reservation)
    }

    fun sendReminders() {
        val reservations = getTomorrowReservations()

        reservations.forEach { reservation ->
            Hibernate.initialize(reservation)
            Hibernate.initialize(reservation.room)

            sendEmail(EmailTemplateType.RESERVATION_REMINDER, EmailTemplateType.RESERVATION_REMINDER, reservation)
        }
    }
}