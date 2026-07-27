package site.komuna.reserve.reservation

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import site.komuna.reserve.common.exception.CannotPerformThatActionException
import site.komuna.reserve.organization.OrganizationService
import site.komuna.reserve.reservation.cancel.CancelReservationService
import site.komuna.reserve.reservation.confirm.ConfirmReservationService
import site.komuna.reserve.reservation.model.ReservationEntity
import site.komuna.reserve.reservation.model.ReservationStatus
import site.komuna.reserve.room.RoomService
import site.komuna.reserve.user.UserService
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime
import kotlin.test.assertEquals

class ReservationServiceTests {

    private val repository: ReservationRepository = mockk()
    private val confirmReservationService: ConfirmReservationService = mockk()
    private val cancelReservationService: CancelReservationService = mockk()
    private val organizationService: OrganizationService = mockk()
    private val roomService: RoomService = mockk()
    private val userService: UserService = mockk()

    private val service = ReservationService(
        repository,
        confirmReservationService,
        cancelReservationService,
        organizationService,
        roomService,
        userService
    )

    @ParameterizedTest
    @EnumSource(
        value = ReservationStatus::class,
        names = [
            "REQUESTED_CANCELLATION",
            "CANCELLED",
            "REJECTED_CANCELLATION"
        ]
    )
    fun requestCancelForCanceledReservation(status: ReservationStatus) {

        val reservation: ReservationEntity = mockk()
        val cancelledByUser: UserEntity = mockk()

        // Arrange
        every {reservation.status } returns status
        every {reservation.startAt } returns OffsetDateTime.now()

        // Act
        // Assert
        assertThrows<CannotPerformThatActionException> {
            service.requestCancelReservation(reservation, cancelledByUser)
        }
    }

    @Test
    fun autoCancellation() {
        // ARRANGE
        val reservation: ReservationEntity = mockk(relaxed = true)
        val cancelledByUser: UserEntity = mockk()
        val systemUser: UserEntity = mockk()

        every { reservation.status } returns ReservationStatus.CONFIRMED
        every { reservation.startAt } returns OffsetDateTime.now().plusDays(12)

        // setter
        every { reservation.status = ReservationStatus.CANCELLED } just Runs

        every { userService.getSystemUser() } returns systemUser
        every {
            cancelReservationService.saveCancelReservationDetails(
                reservation,
                cancelledByUser,
                any(),
                systemUser,
                any()
            )
        } just Runs

        every { repository.save(reservation) } returns reservation

        // ACT
        val result = service.requestCancelReservation(reservation, cancelledByUser)

        // ASSERT
        assertEquals(reservation, result)

        verify {
            reservation.status = ReservationStatus.CANCELLED
        }

        verify {
            userService.getSystemUser()
            cancelReservationService.saveCancelReservationDetails(
                reservation,
                cancelledByUser,
                any(),
                systemUser,
                any()
            )
            repository.save(reservation)
        }
    }

    @Test
    fun shouldSaveCancellationRequestWhenReservationStartsInLessThan6Hours() {
        // Arrange
        val reservation: ReservationEntity = mockk(relaxed = true)
        val cancelledByUser: UserEntity = mockk()

        every { reservation.status } returns ReservationStatus.CONFIRMED
        every { reservation.startAt } returns OffsetDateTime.now().plusHours(6)

        every { reservation.status = ReservationStatus.REQUESTED_CANCELLATION } just Runs
        every { repository.save(reservation) } returns reservation

        every {
            cancelReservationService.saveCancelReservationDetails(
                reservation,
                cancelledByUser,
                any(),
                any(),
                any()
            )
        } just Runs

        // Act
        val result = service.requestCancelReservation(reservation, cancelledByUser)

        // Assert
        assertEquals(reservation, result)

        verify(exactly = 1) {
            reservation.status = ReservationStatus.REQUESTED_CANCELLATION
        }

        verify(exactly = 1) {
            cancelReservationService.saveCancelReservationDetails(
                reservation,
                cancelledByUser,
                any(),
                any(),
                any()
            )
        }

        verify(exactly = 1) {
            repository.save(reservation)
        }

        verify(exactly = 0) {
            userService.getSystemUser()
        }
    }
}