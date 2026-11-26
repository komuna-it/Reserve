package site.komuna.reserve.reservation

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import org.hibernate.Hibernate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.komuna.reserve.audit.AuditService
import site.komuna.reserve.audit.model.AuditType
import site.komuna.reserve.common.httpError.exception.CannotPerformThatActionException
import site.komuna.reserve.common.httpError.exception.ReservationNotFoundException
import site.komuna.reserve.email.EmailService
import site.komuna.reserve.email.model.EmailRecipient
import site.komuna.reserve.email.model.EmailTemplateType
import site.komuna.reserve.organization.OrganizationService
import site.komuna.reserve.organization.model.OrganizationEntity
import site.komuna.reserve.organization.model.SearchOrganizationFilter
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
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale


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
    private val auditService: AuditService,
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun getReservations(
        filter: SearchReservationsFilter,
        pageable: Pageable,
    ): Page<ReservationEntity> {
        return repository.findAll(specification(filter), pageable)
    }

fun specification(filter: SearchReservationsFilter) =
    Specification<ReservationEntity> { root, _, cb ->
        val predicates = mutableListOf<Predicate>()

        filter.reservationId?.let { id ->
            predicates += cb.equal(root.get<Long>("id"), id)
        }

        // Reservation beneficiary
        filter.userId?.let { userId ->
            val userPredicate = cb.equal(
                root.get<UserEntity>("user").get<Long>("id"),
                userId,
            )

            if (filter.private == null && filter.organizationsId.isNotEmpty()) {
                // Calendar visibility:
                // user's reservations OR reservations of their organizations
                val organizationJoin = root.join<ReservationEntity, OrganizationEntity>(
                    "organization",
                    JoinType.LEFT,
                )

                val organizationPredicate =
                    organizationJoin.get<Long>("id").`in`(filter.organizationsId)

                predicates += cb.or(userPredicate, organizationPredicate)
            } else {
                predicates += userPredicate
            }
        }

        // Reservation creator / actor
        filter.actorUserId?.let { actorUserId ->
            predicates += cb.equal(
                root.get<UserEntity>("actor").get<Long>("id"),
                actorUserId,
            )
        }

        when (filter.private) {
            true -> {
                predicates += cb.isNull(root.get<OrganizationEntity>("organization"))
            }

            false -> {
                predicates += cb.isNotNull(root.get<OrganizationEntity>("organization"))

                if (filter.organizationsId.isNotEmpty()) {
                    val organizationJoin = root.join<ReservationEntity, OrganizationEntity>(
                        "organization",
                        JoinType.INNER,
                    )

                    predicates += organizationJoin.get<Long>("id")
                        .`in`(filter.organizationsId)
                }
            }

            null -> {
                if (filter.userId == null && filter.organizationsId.isNotEmpty()) {
                    val organizationJoin = root.join<ReservationEntity, OrganizationEntity>(
                        "organization",
                        JoinType.INNER,
                    )

                    predicates += organizationJoin.get<Long>("id")
                        .`in`(filter.organizationsId)
                }
            }
        }

        if (filter.future) {
            predicates += cb.greaterThanOrEqualTo(
                root.get("startAt"),
                OffsetDateTime.now(ZoneOffset.UTC),
            )
        }

        filter.roomId?.let { roomId ->
            predicates += cb.equal(
                root.get<RoomEntity>("room").get<Long>("id"),
                roomId,
            )
        }

        filter.startAtAfter?.let {
            predicates += cb.greaterThanOrEqualTo(
                root.get<OffsetDateTime>("startAt"),
                it,
            )
        }

        filter.startAtBefore?.let {
            predicates += cb.lessThanOrEqualTo(
                root.get<OffsetDateTime>("startAt"),
                it,
            )
        }

        if (filter.status.isNotEmpty()) {
            predicates += root.get<ReservationStatus>("status").`in`(filter.status)
        }

        if (filter.type.isNotEmpty()) {
            predicates += root.get<ReservationType>("type").`in`(filter.type)
        }

        cb.and(*predicates.toTypedArray())
    }

    fun getTomorrowReservations(): List<ReservationEntity> {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val tomorrow = now.plusDays(1)
        val startAt = tomorrow.withHour(0).withMinute(0).withSecond(0).withNano(0)
        val endAt = tomorrow.withHour(23).withMinute(59).withSecond(59).withNano(999999999)
        return repository.findByStartAtBetween(startAt, endAt)
    }


    fun findById(id: Long): ReservationEntity {
        return repository.findById(id).orElseThrow { ReservationNotFoundException(id) }
    }

    @Transactional
    fun createReservation(request: CreateReservationRequest, currentUser: UserEntity): ReservationEntity {
        prepareCreateRequest(request, currentUser)
        validate(request, currentUser)

        var response = repository.save(ReservationEntity(request))

        response = when (currentUser.role) {
            Role.ADMIN, Role.MANAGER -> {
                // Admin/manager confirms the reservation they create.
                confirmReservation(response, currentUser)
            }

            else -> {
                // Regular users may have their reservations auto-confirmed
                // when they or their organization are trusted.
                confirmReservationIfTrusted(response)
            }
        }

        if (response.status == ReservationStatus.CONFIRMED) {
            emitReservationApproved(response)
        }
        else {
            emitCreatedReservation(response)
        }

        auditService.log(
            type = AuditType.RESERVATION_CREATED,
            actor = currentUser,
            properties = mapOf(
                "reservationId" to response.id,
                "userId" to response.user.id,
                "roomId" to response.room.id,
                "organizationId" to response.organization?.id,
                "status" to response.status
            )
        )

        return response
    }
    /**
     * Reservation could be automatically confirmed if a user or organization is trusted
     */
    fun confirmReservationIfTrusted(reservation: ReservationEntity): ReservationEntity {
        val user = reservation.user
        val organization = reservation.organization

        if (user.trusted) {
            logger.trace { "Automatically confirming reservation ${reservation.id} because user ${user.id} is trusted" }
            return confirmReservationBySystem(reservation)
        }

        if (organization != null && organization.trusted) {
            logger.trace { "Automatically confirming reservation ${reservation.id} because organization ${organization.id} is trusted" }
            return confirmReservationBySystem(reservation)
        }

        return reservation
    }

    fun confirmReservationBySystem(reservation: ReservationEntity): ReservationEntity {
        val systemUser = userService.getSystemUser()
        val response = confirmReservation(reservation, systemUser)
        return response
    }

    fun confirmReservation(
        reservation: ReservationEntity,
        approvedBy: UserEntity
    ): ReservationEntity {

        if (reservation.status != ReservationStatus.CREATED) {
            throw CannotPerformThatActionException(
                "Reservation is not in CREATED status"
            )
        }

        confirmReservationService.saveConfirmReservationDetails(
            reservation,
            approvedBy
        )

        return changeStatus(
            reservation,
            ReservationStatus.CONFIRMED,
            approvedBy
        )
    }

    fun confirmReservation(reservationId: Long, approvedBy: Long): ReservationEntity {
        val user = userService.findById(approvedBy)
        val reservation = findById(reservationId)

        val response = confirmReservation(reservation, user)

        emitReservationApproved(response)

        return response
    }

    fun rejectReservationRequest(
        reservation: ReservationEntity,
        rejectedBy: UserEntity
    ): ReservationEntity {
        if (
            reservation.status == ReservationStatus.REJECTED ||
            reservation.status == ReservationStatus.CANCELLED
        ) {
            throw CannotPerformThatActionException(
                "Current status of reservation: ${reservation.status} cannot request rejection"
            )
        }

        confirmReservationService.saveRejectReservationDetails(
            reservation,
            rejectedBy
        )

        val response = changeStatus(
            reservation,
            ReservationStatus.REJECTED,
            rejectedBy
        )

        emitReservationRejected(response)

        return response
    }

    fun rejectReservationRequest(reservationId: Long, rejectedBy: Long): ReservationEntity {
        val user = userService.findById(rejectedBy)
        val reservation = findById(reservationId)


        return rejectReservationRequest(reservation, user)
    }

    fun requestCancelReservation(
        reservationId: Long,
        cancelledBy: Long
    ): ReservationEntity {
        val reservation = findById(reservationId)
        val user = userService.findById(cancelledBy)

        return requestCancelReservation(reservation, user)
    }

    fun requestCancelReservation(reservation: ReservationEntity, cancelledByUser: UserEntity): ReservationEntity {
        val startAt = reservation.startAt
        val canceledAt = OffsetDateTime.now(ZoneOffset.UTC)
        val canceledBy = cancelledByUser.nick

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

        val allowedHour = settings.getIntValue(SettingsKey.RESERVATION_CANCELLATION_WITHOUT_APPROVAL_HOURS)

        if(time.toHours() > allowedHour) {
            val response = cancelReservationBySystem(reservation, cancelledByUser, canceledAt   )

            emitReservationCancelled(response, canceledBy)
            return response
        }

        val response = saveCancellationRequest(reservation, cancelledByUser, canceledAt)

        emitReservationCancelRequested(response, canceledBy)
        return response
    }

    fun saveCancellationRequest(
        reservation: ReservationEntity,
        cancelledBy: UserEntity,
        canceledAt: OffsetDateTime
    ): ReservationEntity {
        cancelReservationService.saveCancelReservationDetails(
            reservation,
            cancelledBy,
            canceledAt,
            null,
            null
        )

        return changeStatus(
            reservation,
            ReservationStatus.REQUESTED_CANCELLATION,
            cancelledBy
        )
    }

    fun cancelReservationBySystem(reservation: ReservationEntity, canceledBy: UserEntity, canceledAt: OffsetDateTime): ReservationEntity {
        val systemUser = userService.getSystemUser()
        cancelReservationService.saveCancelReservationDetails(reservation,
            canceledBy,
            canceledAt,
            systemUser,
            canceledAt)

        val response = changeStatus(reservation, ReservationStatus.CANCELLED, systemUser)
        sendEmailUsers(EmailTemplateType.RESERVATION_CANCELED_PRIVATE, EmailTemplateType.RESERVATION_CANCELED_ORGANIZATION, response)

        return response
    }

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
        val response = changeStatus(reservation, ReservationStatus.CANCELLED, approvedBy)

        val requestedBy = cancelReservationService.findByReservationId(reservation.id!!).requestedBy.nick

        emitReservationCancelled(response, requestedBy)

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
        val response = changeStatus(reservation, ReservationStatus.REJECTED_CANCELLATION, approvedBy)

        emitReservationRejected(response)

        return response
    }

    fun prepareCreateRequest(
        request: CreateReservationRequest,
        currentUser: UserEntity,
    ) {
        val canReserveForOthers = currentUser.role in setOf(
            Role.ADMIN,
            Role.MANAGER,
        )

        val targetUserId = if (canReserveForOthers && request.userId != null) {
            request.userId!!
        } else {
            requireNotNull(currentUser.id)
        }

        request.userId = targetUserId
        request.user = userService.findById(targetUserId)

        request.actor = currentUser
        request.room = roomService.getRoom(request.roomId)

        request.endAt = request.startAt.plus(request.duration)
        request.reservedAt = OffsetDateTime.now(ZoneOffset.UTC)

        request.organization = request.organizationId?.let {
            organizationService.getOrganization(it)
        }
    }

    fun setPaid(
        reservationId: Long,
        paid: Boolean,
        changedBy: Long
    ): ReservationEntity {
        val reservation = findById(reservationId)
        val actor = userService.findById(changedBy)

        return setPaid(reservation, paid, actor)
    }

    private fun setPaid(
        reservation: ReservationEntity,
        paid: Boolean,
        actor: UserEntity
    ): ReservationEntity {
        val previousPaid = reservation.paid

        if (previousPaid == paid) {
            return reservation
        }

        reservation.paid = paid

        val updatedReservation = repository.save(reservation)

        auditService.log(
            type = AuditType.RESERVATION_PAID_UPDATED,
            actor = actor,
            properties = mapOf(
                "reservationId" to updatedReservation.id,
                "previousPaid" to previousPaid,
                "newPaid" to paid
            )
        )

        return updatedReservation
    }

    fun validate(request: CreateReservationRequest, currentUser: UserEntity) {
        val validator = CreateReservationValidation(organizationService, repository, organizationMemberService, settings)
        validator.validate(request, currentUser)
    }

    fun changeStatus(
        reservation: ReservationEntity,
        newStatus: ReservationStatus,
        actor: UserEntity
    ): ReservationEntity {
        val previousStatus = reservation.status

        if (previousStatus == newStatus) {
            return reservation
        }

        logger.trace {
            "Changing reservation ${reservation.id} status from $previousStatus to $newStatus"
        }

        reservation.status = newStatus

        val updatedReservation = repository.save(reservation)

        auditService.log(
            type = AuditType.RESERVATION_STATUS_UPDATED,
            actor = actor,
            properties = mapOf(
                "reservationId" to updatedReservation.id,
                "previousStatus" to previousStatus.name,
                "newStatus" to newStatus.name,
                "userId" to updatedReservation.user.id,
                "roomId" to updatedReservation.room.id,
                "organizationId" to updatedReservation.organization?.id
            )
        )

        return updatedReservation
    }

    fun getReservationDto(reservation: ReservationEntity): ReservationDto {
        val price = pricingService.getPrice(reservation)
        return ReservationDto(reservation, price)
    }

    // EMAILS
    // =========================================================================================================================================

    private fun emitCreatedReservation(reservation: ReservationEntity) {
        sseService.broadcast(ReserveEvents.RESERVATION_CREATED, ReservationDto(reservation))
        sendEmailUsers(EmailTemplateType.RESERVATION_CREATED_PRIVATE, EmailTemplateType.RESERVATION_CREATED_ORGANIZATION, reservation)
    }

    // approve or reject reservation request
    private fun emitReservationApproved(reservation: ReservationEntity) {
        sseService.broadcast(ReserveEvents.RESERVATION_CONFIRMED, ReservationDto(reservation))
        sendEmailUsers(EmailTemplateType.RESERVATION_CONFIRMED, EmailTemplateType.RESERVATION_CONFIRMED, reservation)
    }

    private fun emitReservationRejected(reservation: ReservationEntity) {
        sseService.broadcast(ReserveEvents.RESERVATION_REJECTED, ReservationDto(reservation))
        sendEmailUsers(EmailTemplateType.RESERVATION_REJECTED, EmailTemplateType.RESERVATION_REJECTED, reservation)
    }

    // approve or reject cancel reservation request
    private fun emitReservationCancelRequested(reservation: ReservationEntity, requestedBy: String) {
        sseService.broadcast(ReserveEvents.RESERVATION_CANCEL_REQUESTED, ReservationDto(reservation))

        val model = mutableMapOf<String, Any>()
        model["requestedBy"] = requestedBy

        sendEmailManagers(EmailTemplateType.RESERVATION_CANCEL_REQUESTED, reservation, model)
    }

    private fun emitReservationCancelled(reservation: ReservationEntity, requestedBy: String) {
        sseService.broadcast(ReserveEvents.RESERVATION_CANCELED, ReservationDto(reservation))

        val model = mutableMapOf<String, Any>()
        model["requestedBy"] = requestedBy

        sendEmailUsers(EmailTemplateType.RESERVATION_CANCELED_PRIVATE, EmailTemplateType.RESERVATION_CANCELED_ORGANIZATION, reservation, model)
    }

    /**
     * Method will email all users who have reservations in the next 24 day
     */
    fun emitReservationReminders() {
        val reservations = getTomorrowReservations()

        reservations.forEach { reservation ->
            Hibernate.initialize(reservation)
            Hibernate.initialize(reservation.room)

            sendEmailUsers(EmailTemplateType.RESERVATION_REMINDER, EmailTemplateType.RESERVATION_REMINDER, reservation)
        }
    }

    // Sender
    private fun sendEmailUsers(privateTemplate: EmailTemplateType, organizationTemplate: EmailTemplateType, reservation: ReservationEntity, model: MutableMap<String, Any> = mutableMapOf<String, Any>()) {
        val formatter = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.of("pl", "PL"))
        val zoneId = ZoneId.of("Europe/Warsaw")

        val user = reservation.user
        val startAt = reservation.startAt.atZoneSameInstant(zoneId)
        val endAt = reservation.endAt.atZoneSameInstant(zoneId)
        val duration = Duration.between(startAt, endAt)

        model["roomName"] = reservation.room.name
        model["duration"] = duration.toHoursPart()
        model["startAt"] = startAt.format(formatter)
        model["endAt"] = endAt.format(formatter)


        // Handle private reservation
        if(reservation.organization == null) {
            val recipient = EmailRecipient(user)

            emailService.sendEmailToUser(privateTemplate, recipient, model)

            auditService.log(
                type = AuditType.EMAIL_SENT_TO_USERS,
                actor = null,
                properties = mapOf(
                    "reservationId" to reservation.id,
                    "emailType" to privateTemplate.name,
                    "recipientUserIds" to listOfNotNull(user.id),
                    "recipientCount" to 1
                )
            )
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

            auditService.log(
                type = AuditType.EMAIL_SENT_TO_USERS,
                actor = null,
                properties = mapOf(
                    "reservationId" to reservation.id,
                    "emailType" to organizationTemplate.name,
                    "organizationId" to organization.id,
                    "recipientUserIds" to members.mapNotNull { it.id },
                    "recipientCount" to recipients.size ,
                )
            )
        }


    }

    private fun sendEmailManagers(
        type: EmailTemplateType,
        reservation: ReservationEntity,
        model: MutableMap<String, Any> = mutableMapOf()
    ) {
        val formatter = DateTimeFormatter.ofPattern(
            "d MMM HH:mm",
            Locale.of("pl", "PL")
        )
        val zoneId = ZoneId.of("Europe/Warsaw")

        val startAt = reservation.startAt.atZoneSameInstant(zoneId)
        val endAt = reservation.endAt.atZoneSameInstant(zoneId)
        val duration = Duration.between(startAt, endAt)

        model["roomName"] = reservation.room.name
        model["duration"] = duration.toHoursPart()
        model["startAt"] = startAt.format(formatter)
        model["endAt"] = endAt.format(formatter)

        val managers = userService.getManagers()

        val recipients = managers.map { manager ->
            EmailRecipient(manager)
        }

        recipients.forEach { recipient ->
            emailService.sendEmailToUser(type, recipient, model)
        }

        auditService.log(
            type = AuditType.EMAIL_SENT_TO_MANAGERS,
            actor = null,
            properties = mapOf(
                "reservationId" to reservation.id,
                "emailType" to type.name,
                "recipientUserIds" to managers.mapNotNull { it.id },
                "recipientCount" to recipients.size
            )
        )
    }

    fun getReservationSummary(): Map<ReservationStatus, Long> {
        val counts = repository.countReservationsByStatus()
        return counts.associate { it.status to it.count }
    }

    @Transactional(readOnly = true)
    fun getCalendarReservations(
        startAtAfter: OffsetDateTime,
        startAtBefore: OffsetDateTime,
        roomId: Long?,
        pageable: Pageable,
        authentication: Authentication?
    ): Page<ReservationDto> {

        val currentUser: UserEntity? = authentication
            ?.name
            ?.toLongOrNull()
            ?.let { userId -> userService.findById(userId) }

        val isAdminOrManager = currentUser?.role in setOf(
            Role.ADMIN,
            Role.MANAGER
        )

        val visibleStatuses = listOf(
            ReservationStatus.REQUESTED_CANCELLATION,
            ReservationStatus.CREATED,
            ReservationStatus.CONFIRMED,
            ReservationStatus.REJECTED_CANCELLATION
        )

        val filter = when {
            // Administrators and managers can see all reservations.
            isAdminOrManager -> SearchReservationsFilter(
                roomId = roomId,
                startAtAfter = startAtAfter,
                startAtBefore = startAtBefore,
                status = visibleStatuses.toMutableList()
            )

            // Regular users can see their private reservations
            // and reservations belonging to their organizations.
            currentUser != null -> {
                val userId = requireNotNull(currentUser.id)

                val organizationIds = organizationService
                    .collectTargetOrganizationIds(
                        SearchOrganizationFilter(userId = userId)
                    )
                    .toMutableList()

                SearchReservationsFilter(
                    roomId = roomId,
                    startAtAfter = startAtAfter,
                    startAtBefore = startAtBefore,
                    status = visibleStatuses.toMutableList()
                )
            }

            // Guests can see calendar entries, but only selected statuses.
            else -> SearchReservationsFilter(
                roomId = roomId,
                startAtAfter = startAtAfter,
                startAtBefore = startAtBefore,
                status = visibleStatuses.toMutableList()
            )
        }

        return repository.findAll(
            specification(filter),
            pageable
        ).map { reservation ->

            val organization = reservation.organization

            val canSeeIdentifiers = when {
                isAdminOrManager -> true

                currentUser == null -> false

                // Private reservation: only its beneficiary can see details.
                organization == null ->
                    reservation.user.id == currentUser.id

                // Organization reservation: members and owners can see details.
                else ->
                    organizationService.isMember(currentUser, organization) ||
                            organizationService.isOwner(currentUser, organization)
            }

            val dto = ReservationDto(
                reservation,
                pricingService.getPrice(reservation)
            )

            if (canSeeIdentifiers) {
                dto
            } else {
                dto.copy(
                    id = null,
                    status = null,
                    userId = null,
                    organizationId = null,
                    actorUserId = null,
                    paid = null,
                    price = null
                )
            }
        }
    }
}