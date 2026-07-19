package site.komuna.reserve.reservation

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import site.komuna.reserve.common.exception.CannotPerformThatActionException
import site.komuna.reserve.organization.OrganizationService
import site.komuna.reserve.organization.model.OrganizationEntity
import site.komuna.reserve.reservation.model.CreateReservationRequest
import site.komuna.reserve.room.RoomService
import site.komuna.reserve.user.UserService
import site.komuna.reserve.user.model.UserEntity
import java.time.Duration
import java.time.OffsetDateTime
import kotlin.test.assertTrue

class ReservationServiceTests {

    val repository = mockk<ReservationRepository>()
    val organizationService = mockk<OrganizationService>()
    val roomService = mockk<RoomService>()
    val userService = mockk<UserService>()

    // Test method validateOrganizationMembership
    @Test
    fun memberOfOrganizationShouldReturnOrganization() {
        // Arrange
        val service = ReservationService(repository, organizationService, roomService, userService)

        val user = mockk<UserEntity>()
        val request = mockk<CreateReservationRequest>()
        val organization = mockk<OrganizationEntity>()

        every { organizationService.getOrganization(1L) } returns organization
        every { organizationService.isMember(user, organization) } returns true
        every { request.organizationId } returns 1L
        every { request.reservedByUser } returns user
        every { request.organization } returns organization

        // Act
        val result = service.validateOrganizationMembership(request)

        // Assert
        assertTrue(result)
        verifySequence {
            organizationService.isMember(user, organization)
        }
    }

    @Test
    fun throwExceptionWhenUserIsNotMemberOfOrganization() {

        // Arrange
        val service = ReservationService(repository, organizationService, roomService, userService)

        val user = mockk<UserEntity>()
        val request = mockk<CreateReservationRequest>()
        val organization = mockk<OrganizationEntity>()

        every { organizationService.isMember(user, organization) } returns false
        every { user.id } returns 1L
        every { organization.name } returns "Test organization"
        every { request.organization } returns organization
        every { request.reservedByUser } returns user

        // Act and Assert
        assertThrows<CannotPerformThatActionException> {
            service.validateOrganizationMembership(request)
        }

        verify {
            organizationService.isMember(user, organization)
        }
    }

    @Test
    fun requestNotConnectedToOrganization() {

        // Arrange
        val service = ReservationService(repository, organizationService, roomService, userService)

        val user = mockk<UserEntity>()
        val request = mockk<CreateReservationRequest>()
        val organization = mockk<OrganizationEntity>()

        every { request.organizationId } returns null
        every { organizationService.getOrganization(1L) } returns organization
        every { organizationService.isMember(user, organization) } returns true
        every { request.reservedByUser } returns user
        every { request.organizationId } returns null
        every { request.organization } returns null
        every { request.reservedByUser } returns user

        // Act
        val result = service.validateOrganizationMembership(request)

        // Assert
        assertTrue(result)
    }

    // Test method isRoomAvailable
    @Test
    fun roomIsTaken() {
        // Arrange
        val service = ReservationService(repository, organizationService, roomService, userService)

        val request = mockk<CreateReservationRequest>()

        every {repository.findOverlappingReservations(1L, any(), any())} returns listOf(mockk())
        every {request.room!!.id} returns 1L
        every {request.room!!.name} returns "Test room"
        every {request.startAt} returns OffsetDateTime.now()
        every {request.endAt} returns OffsetDateTime.now().plusHours(1)

        // Act
        // Assert
        assertThrows<CannotPerformThatActionException> {
            service.isRoomAvailable(request)
        }
    }

    @Test
    fun roomIsAvailable() {
        // Arrange
        val service = ReservationService(repository, organizationService, roomService, userService)

        val request = mockk<CreateReservationRequest>()

        every {repository.findOverlappingReservations(1L, any(), any())} returns listOf()
        every {request.room!!.id} returns 1L
        every {request.room!!.name} returns "Test room"
        every {request.startAt} returns OffsetDateTime.now()
        every {request.endAt} returns OffsetDateTime.now().plusHours(1)

        // Act
        val result = service.isRoomAvailable(request)
        // Assert
        assertTrue(result)
    }

    // Test method isReservationInFuture
    @Test
    fun reservationInTheFuture() {

        // Arrange
        val service = ReservationService(repository, organizationService, roomService, userService)

        val reservation = mockk<CreateReservationRequest>()

        every {reservation.startAt} returns OffsetDateTime.now().plusDays(1)

        // Act
        val result = service.isStartAtInFuture(reservation)

        // Assert
        assertTrue(result)
    }

    @Test
    fun reservationNotInTheFuture() {

        // Arrange
        val service = ReservationService(repository, organizationService, roomService, userService)

        val reservation = mockk<CreateReservationRequest>()

        every {reservation.startAt} returns OffsetDateTime.now().minusDays(1)

        // Act
        // Assert
        assertThrows<CannotPerformThatActionException> {
            service.isStartAtInFuture(reservation)
        }
    }

    // Test method isReservationInAllowedRange
    @Test
    fun reservationInAllowedRange() {
        // Arrange
        val service = ReservationService(repository, organizationService, roomService, userService)

        val reservation = mockk<CreateReservationRequest>()

        every {reservation.startAt} returns OffsetDateTime.now().withHour(11).withMinute(0)
        every {reservation.duration} returns Duration.ofHours(1)

        // Act
        val result = service.isReservationInAllowedRange(reservation)

        // Assert
        assertTrue(result)
    }

    @Test
    fun reservationNotInAllowedRangeBeforeStart() {
        // Arrange
        val service = ReservationService(repository, organizationService, roomService, userService)

        val reservation = mockk<CreateReservationRequest>()

        every {reservation.startAt} returns OffsetDateTime.now().withHour(9).withMinute(0)
        every {reservation.duration} returns Duration.ofHours(1)

        // Act
        // Assert
        assertThrows<CannotPerformThatActionException> {
            service.isReservationInAllowedRange(reservation)
        }
    }

    @Test
    fun reservationNotInAllowedRangeAfterStart() {
        // Arrange
        val service = ReservationService(repository, organizationService, roomService, userService)

        val reservation = mockk<CreateReservationRequest>()

        every {reservation.startAt} returns OffsetDateTime.now().withHour(20).withMinute(0)
        every {reservation.duration} returns Duration.ofHours(3)

        // Act
        // Assert
        assertThrows<CannotPerformThatActionException> {
            service.isReservationInAllowedRange(reservation)
        }
    }

    @Test
    fun reservationNotInAllowedInterval() {
        // Arrange
        val service = ReservationService(repository, organizationService, roomService, userService)

        val reservation = mockk<CreateReservationRequest>()

        every {reservation.startAt} returns OffsetDateTime.now().withHour(20).withMinute(25)
        every {reservation.duration} returns Duration.ofHours(3)

        // Act
        // Assert
        assertThrows<CannotPerformThatActionException> {
            service.isReservationInAllowedRange(reservation)
        }
    }

    // Test method isDurationValid
    @Test
    fun durationIsWithinAllowedRange() {
        // Arrange
        val service = ReservationService(repository, organizationService, roomService, userService)

        val request = mockk<CreateReservationRequest>()

        every {request.duration} returns Duration.ofHours(2)

        // Act
        val result = service.isDurationValid(request)

        // Assert
        assertTrue(result)
    }

    @Test
    fun durationIsNotWithinAllowedRange() {
        // Arrange
        val service = ReservationService(repository, organizationService, roomService, userService)

        val request = mockk<CreateReservationRequest>()

        every {request.duration} returns Duration.ofMinutes(90)

        // Act
        // Assert
        assertThrows<CannotPerformThatActionException> {
            service.isDurationValid(request)
        }
    }
}