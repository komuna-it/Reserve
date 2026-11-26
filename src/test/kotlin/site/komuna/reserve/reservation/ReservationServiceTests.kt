package site.komuna.reserve.reservation

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import site.komuna.reserve.audit.AuditService
import site.komuna.reserve.common.httpError.exception.CannotPerformThatActionException
import site.komuna.reserve.email.EmailService
import site.komuna.reserve.organization.OrganizationService
import site.komuna.reserve.organization.organizationMember.OrganizationMemberService
import site.komuna.reserve.reservation.cancel.CancelReservationService
import site.komuna.reserve.reservation.confirm.ConfirmReservationService
import site.komuna.reserve.reservation.model.ReservationEntity
import site.komuna.reserve.reservation.model.ReservationStatus
import site.komuna.reserve.room.RoomService
import site.komuna.reserve.room.model.RoomEntity
import site.komuna.reserve.room.pricing.PricingService
import site.komuna.reserve.settings.SettingsService
import site.komuna.reserve.settings.model.SettingsKey
import site.komuna.reserve.sse.SseService
import site.komuna.reserve.user.UserService
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime
import kotlin.test.assertEquals

class ReservationServiceTests {

    private val repository = mockk<ReservationRepository>(relaxed = true)
    private val confirmReservationService = mockk<ConfirmReservationService>(relaxed = true)
    private val cancelReservationService = mockk<CancelReservationService>(relaxed = true)
    private val organizationService = mockk<OrganizationService>(relaxed = true)
    private val roomService = mockk<RoomService>(relaxed = true)
    private val userService = mockk<UserService>(relaxed = true)
    private val sseService = mockk<SseService>(relaxed = true)
    private val settings = mockk<SettingsService>(relaxed = true)
    private val emailService = mockk<EmailService>(relaxed = true)
    private val pricingService = mockk<PricingService>(relaxed = true)
    private val organizationMemberService = mockk<OrganizationMemberService>(relaxed = true)
    private val auditService = mockk<AuditService>(relaxed = true)

    private val service = ReservationService(
        repository = repository,
        confirmReservationService = confirmReservationService,
        cancelReservationService = cancelReservationService,
        organizationService = organizationService,
        roomService = roomService,
        userService = userService,
        sseService = sseService,
        settings = settings,
        emailService = emailService,
        pricingService = pricingService,
        organizationMemberService = organizationMemberService,
        auditService = auditService,
    )

    private fun createReservation(
        status: ReservationStatus,
        startAt: OffsetDateTime,
    ): ReservationEntity {
        val reservation = mockk<ReservationEntity>(relaxed = true)
        val user = mockk<UserEntity>(relaxed = true)
        val room = mockk<RoomEntity>(relaxed = true)

        every { reservation.id } returns 1L
        every { reservation.status } returns status
        every { reservation.startAt } returns startAt
        every { reservation.endAt } returns startAt.plusHours(2)
        every { reservation.user } returns user
        every { reservation.room } returns room
        every { reservation.organization } returns null

        every { user.id } returns 10L
        every { user.nick } returns "Test user"

        every { room.id } returns 1L
        every { room.name } returns "Test room"

        every { repository.save(reservation) } returns reservation

        return reservation
    }

    @ParameterizedTest
    @EnumSource(
        value = ReservationStatus::class,
        names = [
            "REQUESTED_CANCELLATION",
            "CANCELLED",
            "REJECTED_CANCELLATION",
        ],
    )
    fun requestCancelForCanceledReservation(status: ReservationStatus) {
        // Arrange
        val reservation = mockk<ReservationEntity>()
        val cancelledByUser = mockk<UserEntity>()

        every { reservation.status } returns status

        // Act & Assert
        assertThrows<CannotPerformThatActionException> {
            service.requestCancelReservation(reservation, cancelledByUser)
        }

        verify(exactly = 0) {
            cancelReservationService.saveCancelReservationDetails(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }

        verify(exactly = 0) {
            repository.save(any<ReservationEntity>())
        }

        verify(exactly = 0) {
            userService.getSystemUser()
        }
    }

    @Test
    fun autoCancellationWhenReservationStartsMoreThan24HoursLater() {
        // Arrange
        val startAt = OffsetDateTime.now().plusDays(12)
        val reservation = createReservation(
            status = ReservationStatus.CONFIRMED,
            startAt = startAt,
        )

        val cancelledByUser = mockk<UserEntity>(relaxed = true)
        val systemUser = mockk<UserEntity>(relaxed = true)

        every { userService.getSystemUser() } returns systemUser

        every {
            settings.getIntValue(
                SettingsKey.RESERVATION_CANCELLATION_WITHOUT_APPROVAL_HOURS,
            )
        } returns 24

        every {
            cancelReservationService.saveCancelReservationDetails(
                reservation,
                cancelledByUser,
                any(),
                systemUser,
                any(),
            )
        } just Runs

        // Act
        val result = service.requestCancelReservation(
            reservation = reservation,
            cancelledByUser = cancelledByUser,
        )

        // Assert
        assertEquals(reservation, result)

        verify(exactly = 1) {
            userService.getSystemUser()
        }

        verify(exactly = 1) {
            cancelReservationService.saveCancelReservationDetails(
                reservation,
                cancelledByUser,
                any(),
                systemUser,
                any(),
            )
        }

        verify(exactly = 1) {
            repository.save(reservation)
        }

        verify(exactly = 0) {
            cancelReservationService.updateCancelReservationDetails(any(), any())
        }
    }

    @Test
    fun shouldSaveCancellationRequestWhenReservationStartsInLessThan24Hours() {
        // Arrange
        val startAt = OffsetDateTime.now().plusHours(5)
        val reservation = createReservation(
            status = ReservationStatus.CONFIRMED,
            startAt = startAt,
        )

        val cancelledByUser = mockk<UserEntity>(relaxed = true)

        every {
            settings.getIntValue(
                SettingsKey.RESERVATION_CANCELLATION_WITHOUT_APPROVAL_HOURS,
            )
        } returns 24

        every {
            cancelReservationService.saveCancelReservationDetails(
                reservation,
                cancelledByUser,
                any(),
                null,
                null,
            )
        } just Runs

        // Act
        val result = service.requestCancelReservation(
            reservation = reservation,
            cancelledByUser = cancelledByUser,
        )

        // Assert
        assertEquals(reservation, result)

        verify(exactly = 0) {
            userService.getSystemUser()
        }

        verify(exactly = 1) {
            cancelReservationService.saveCancelReservationDetails(
                reservation,
                cancelledByUser,
                any(),
                null,
                null,
            )
        }

        verify(exactly = 1) {
            repository.save(reservation)
        }
    }
}