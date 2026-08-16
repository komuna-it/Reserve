package site.komuna.reserve.organization

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import site.komuna.reserve.common.httpError.exception.CannotPerformThatActionException
import site.komuna.reserve.common.httpError.exception.OrganizationMemberNotFoundException
import site.komuna.reserve.organization.model.OrganizationEntity
import site.komuna.reserve.organization.organizationMember.OrganizationMemberRole
import site.komuna.reserve.organization.organizationMember.OrganizationMemberService
import site.komuna.reserve.organization.organizationMember.model.OrganizationMemberEntity
import site.komuna.reserve.user.UserService
import site.komuna.reserve.user.model.UserEntity
import java.util.Optional

class OrganizationServiceTest {

    private val repository = mockk<OrganizationRepository>()
    private val userService = mockk<UserService>()
    private val organizationMemberService = mockk<OrganizationMemberService>()

    val service = OrganizationService(repository, organizationMemberService, userService)

    @Test
    fun addMemberShouldAddUserToOrganization() {
        // Arrange


        val organization = mockk<OrganizationEntity>()
        val user = mockk<UserEntity>()
        val addedBy = mockk<UserEntity>()
        val member = mockk<OrganizationMemberEntity>()

        every { repository.findById(1) } returns Optional.of(organization)

        every { userService.findById(2) } returns user
        every { userService.findById(3) } returns addedBy

        every {
            organizationMemberService.getOrganizationMember(
                addedBy,
                organization
            )
        } returns mockk()

        every {
            organizationMemberService.addMember(
                organization,
                user,
                addedBy
            )
        } returns member

        // Act
        val result = service.addMember(
            userId = 2,
            organizationId = 1,
            addedBy = 3
        )

        // Assert
        assertEquals(member, result)

        verify(exactly = 1) {
            repository.findById(1)

            userService.findById(2)
            userService.findById(3)

            organizationMemberService.getOrganizationMember(
                addedBy,
                organization
            )

            organizationMemberService.addMember(
                organization,
                user,
                addedBy
            )
        }
    }

    @Test
    fun addMemberShouldThrowWhenUserIsNotMember() {
        // Arrange
        val organization = mockk<OrganizationEntity> {
            every { name } returns "My organization"
        }

        val user = mockk<UserEntity> {
            every { nick } returns "John"
        }

        val addedBy = mockk<UserEntity> {
            every { nick } returns "Kate"
        }

        every { repository.findById(1) } returns Optional.of(organization)

        every { userService.findById(2) } returns user
        every { userService.findById(3) } returns addedBy

        every {
            organizationMemberService.getOrganizationMember(
                addedBy,
                organization
            )
        } throws OrganizationMemberNotFoundException(2, 1)

        // Act + Assert
        assertThrows(CannotPerformThatActionException::class.java) {
            service.addMember(
                userId = 2,
                organizationId = 1,
                addedBy = 3
            )
        }

        verify(exactly = 0) {
            organizationMemberService.addMember(any(), any(), any())
        }
    }

    @Test
    fun removeMemberShouldRemoveUserFromOrganization() {
        // Arrange
        val organization = mockk<OrganizationEntity>()
        val user = mockk<UserEntity>()
        val removedBy = mockk<UserEntity>()

        every { repository.findById(1) } returns Optional.of(organization)

        every { userService.findById(2) } returns user
        every { userService.findById(3) } returns removedBy

        val ownerMembership = mockk<OrganizationMemberEntity> {
            every { role } returns OrganizationMemberRole.OWNER
        }

        val memberMembership = mockk<OrganizationMemberEntity> {
            every { role } returns OrganizationMemberRole.MEMBER
        }

        every {
            organizationMemberService.getOrganizationMember(
                removedBy,
                organization
            )
        } returns ownerMembership

        every {
            organizationMemberService.getOrganizationMember(
                user,
                organization
            )
        } returns memberMembership

        every {
            organizationMemberService.removeMember(
                organization,
                user
            )
        } returns Unit

        // Act
        service.removeMember(
            userId = 2,
            organizationId = 1,
            removedBy = 3
        )

        // Assert
        verify(exactly = 1) {
            organizationMemberService.removeMember(
                organization,
                user
            )
        }
    }

    @Test
    fun removeMemberShouldThrowWhenRemovedByIsNotOwner() {
        // Arrange

        val organization = mockk<OrganizationEntity> {
            every { name } returns "Organization"
        }

        val user = mockk<UserEntity> {
            every { nick } returns "John"
        }

        val removedBy = mockk<UserEntity> {
            every { nick } returns "Kate"
        }

        val memberMembership = mockk<OrganizationMemberEntity> {
            every { role } returns OrganizationMemberRole.MEMBER
        }

        every { repository.findById(1) } returns Optional.of(organization)

        every { userService.findById(2) } returns user
        every { userService.findById(3) } returns removedBy

        every {
            organizationMemberService.getOrganizationMember(
                removedBy,
                organization
            )
        } returns memberMembership

        // Act + Assert
        assertThrows(CannotPerformThatActionException::class.java) {
            service.removeMember(
                userId = 2,
                organizationId = 1,
                removedBy = 3
            )
        }

        verify(exactly = 0) {
            organizationMemberService.removeMember(any(), any())
        }
    }

    @Test
    fun removeMemberShouldThrowWhenRemovingOwner() {
        // Arrange

        val organization = mockk<OrganizationEntity>()

        val user = mockk<UserEntity>()
        val removedBy = mockk<UserEntity>()

        val ownerMembership = mockk<OrganizationMemberEntity> {
            every { role } returns OrganizationMemberRole.OWNER
        }

        every { repository.findById(1) } returns Optional.of(organization)

        every { userService.findById(2) } returns user
        every { userService.findById(3) } returns removedBy

        every {
            organizationMemberService.getOrganizationMember(
                removedBy,
                organization
            )
        } returns ownerMembership

        every {
            organizationMemberService.getOrganizationMember(
                user,
                organization
            )
        } returns ownerMembership

        // Act + Assert
        assertThrows(CannotPerformThatActionException::class.java) {
            service.removeMember(
                userId = 2,
                organizationId = 1,
                removedBy = 3
            )
        }

        verify(exactly = 0) {
            organizationMemberService.removeMember(any(), any())
        }
    }
}