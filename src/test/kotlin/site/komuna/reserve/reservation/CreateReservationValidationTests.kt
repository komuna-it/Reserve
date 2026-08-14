package site.komuna.reserve.reservation

import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import site.komuna.reserve.common.exception.CannotPerformThatActionException
import site.komuna.reserve.common.exception.OrganizationMemberNotFoundException
import site.komuna.reserve.organization.OrganizationService
import site.komuna.reserve.organization.model.OrganizationEntity
import site.komuna.reserve.organization.organizationMember.OrganizationMemberService
import site.komuna.reserve.reservation.model.CreateReservationRequest
import site.komuna.reserve.reservation.validation.CreateReservationValidation
import site.komuna.reserve.settings.SettingsService
import site.komuna.reserve.user.model.UserEntity
import java.time.Duration
import java.time.OffsetDateTime
import kotlin.test.assertTrue

class CreateReservationValidationTests {

    val reservationRepository = mockk<ReservationRepository>()
    val organizationService = mockk<OrganizationService>()
    val organizationMemberService = mockk<OrganizationMemberService>()
    val settingsService = mockk<SettingsService>()

    val validator = CreateReservationValidation(organizationService, reservationRepository, organizationMemberService, settingsService)

    // Test method validateOrganizationMembership
//    @Test
//    fun memberOfOrganizationShouldReturnOrganization() {
//        // Arrange
//
//        val user = mockk<UserEntity>()
//        val request = mockk<CreateReservationRequest>()
//        val organization = mockk<OrganizationEntity>()
//
//        every { organizationService.getOrganization(1L) } returns organization
//        every { organizationService.isMember(user, organization) } returns true
//        every { request.organizationId } returns 1L
//        every { request.reservedByUser } returns user
//        every { request.organization } returns organization
//
//        // Act
//        val result = validator.validateOrganizationMembership(request)
//
//        // Assert
//        assertTrue(result)
//        verifySequence {
//            organizationService.isMember(user, organization)
//        }
//    }

//    @Test
//    fun throwExceptionWhenUserIsNotMemberOfOrganization() {
//
//        // Arrange
//
//        val user = mockk<UserEntity>()
//        val request = mockk<CreateReservationRequest>()
//        val organization = mockk<OrganizationEntity>()
//
//        every { organizationService.isMember(user, organization) } throws OrganizationMemberNotFoundException(1L, 1L)
//        every { user.id } returns 1L
//        every { organization.name } returns "Test organization"
//        every { request.organization } returns organization
//        every { request.reservedByUser } returns user
//
//        // Act and Assert
//        assertThrows<CannotPerformThatActionException> {
//            validator.validateOrganizationMembership(request)
//        }
//    }

//    @Test
//    fun requestNotConnectedToOrganization() {
//
//        // Arrange
//
//        val user = mockk<UserEntity>()
//        val request = mockk<CreateReservationRequest>()
//        val organization = mockk<OrganizationEntity>()
//
//        every { request.organizationId } returns null
//        every { organizationService.getOrganization(1L) } returns organization
//        every { organizationService.isMember(user, organization) } returns true
//        every { request.reservedByUser } returns user
//        every { request.organizationId } returns null
//        every { request.organization } returns null
//        every { request.reservedByUser } returns user
//
//        // Act
//        val result = validator.validateOrganizationMembership(request)
//
//        // Assert
//        assertTrue(result)
//    }

    // Test method isRoomAvailable
    @Test
    fun roomIsTaken() {
        // Arrange

        val request = mockk<CreateReservationRequest>()

        every {reservationRepository.findOverlappingReservations(1L, any(), any())} returns listOf(mockk())
        every {request.room!!.id} returns 1L
        every {request.room!!.name} returns "Test room"
        every {request.startAt} returns OffsetDateTime.now()
        every {request.endAt} returns OffsetDateTime.now().plusHours(1)

        // Act
        // Assert
        assertThrows<CannotPerformThatActionException> {
            validator.isRoomAvailable(request)
        }
    }

    @Test
    fun roomIsAvailable() {
        // Arrange

        val request = mockk<CreateReservationRequest>()

        every {reservationRepository.findOverlappingReservations(1L, any(), any())} returns listOf()
        every {request.room!!.id} returns 1L
        every {request.room!!.name} returns "Test room"
        every {request.startAt} returns OffsetDateTime.now()
        every {request.endAt} returns OffsetDateTime.now().plusHours(1)

        // Act
        val result = validator.isRoomAvailable(request)
        // Assert
        assertTrue(result)
    }

    // Test method isReservationInFuture
    @Test
    fun reservationInTheFuture() {

        // Arrange

        val reservation = mockk<CreateReservationRequest>()

        every {reservation.startAt} returns OffsetDateTime.now().plusDays(1)

        // Act
        val result = validator.isStartAtInFuture(reservation)

        // Assert
        assertTrue(result)
    }

    @Test
    fun reservationNotInTheFuture() {

        // Arrange

        val reservation = mockk<CreateReservationRequest>()

        every {reservation.startAt} returns OffsetDateTime.now().minusDays(1)

        // Act
        // Assert
        assertThrows<CannotPerformThatActionException> {
            validator.isStartAtInFuture(reservation)
        }
    }

    // Test method isReservationInAllowedRange
    @Test
    fun reservationInAllowedRange() {
        // Arrange

        val reservation = mockk<CreateReservationRequest>()

        every {reservation.startAt} returns OffsetDateTime.now().withHour(11).withMinute(0)
        every {reservation.duration} returns Duration.ofHours(1)

        // Act
        val result = validator.isReservationInAllowedRange(reservation)

        // Assert
        assertTrue(result)
    }

    @Test
    fun reservationNotInAllowedRangeBeforeStart() {
        // Arrange

        val reservation = mockk<CreateReservationRequest>()

        every {reservation.startAt} returns OffsetDateTime.now().withHour(9).withMinute(0)
        every {reservation.duration} returns Duration.ofHours(1)

        // Act
        // Assert
        assertThrows<CannotPerformThatActionException> {
            validator.isReservationInAllowedRange(reservation)
        }
    }

    @Test
    fun reservationNotInAllowedRangeAfterStart() {
        // Arrange

        val reservation = mockk<CreateReservationRequest>()

        every {reservation.startAt} returns OffsetDateTime.now().withHour(20).withMinute(0)
        every {reservation.duration} returns Duration.ofHours(3)

        // Act
        // Assert
        assertThrows<CannotPerformThatActionException> {
            validator.isReservationInAllowedRange(reservation)
        }
    }

    @Test
    fun reservationNotInAllowedInterval() {
        // Arrange

        val reservation = mockk<CreateReservationRequest>()

        every {reservation.startAt} returns OffsetDateTime.now().withHour(20).withMinute(25)
        every {reservation.duration} returns Duration.ofHours(3)

        // Act
        // Assert
        assertThrows<CannotPerformThatActionException> {
            validator.isReservationInAllowedRange(reservation)
        }
    }

    // Test method isDurationValid
    @Test
    fun durationIsWithinAllowedRange() {
        // Arrange

        val request = mockk<CreateReservationRequest>()

        every {request.duration} returns Duration.ofHours(2)

        // Act
        val result = validator.isDurationValid(request)

        // Assert
        assertTrue(result)
    }

    @Test
    fun durationIsNotWithinAllowedRange() {
        // Arrange

        val request = mockk<CreateReservationRequest>()

        every {request.duration} returns Duration.ofMinutes(90)

        // Act
        // Assert
        assertThrows<CannotPerformThatActionException> {
            validator.isDurationValid(request)
        }
    }
}