package site.komuna.reserve.organization.organizationMember

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import site.komuna.reserve.common.exception.CannotPerformThatActionException
import site.komuna.reserve.common.exception.OrganizationMemberNotFoundException
import site.komuna.reserve.organization.model.OrganizationEntity
import site.komuna.reserve.organization.organizationMember.model.OrganizationMemberEntity
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class OrganizationMemberService(
    private val repository: OrganizationMemberRepository,
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun addMember(organization: OrganizationEntity, user: UserEntity, addedBy: UserEntity): OrganizationMemberEntity {
        logger.info { "${addedBy.nick} added ${user.nick} to ${organization.name}" }

        return addUser(organization, user, addedBy, OrganizationMemberRole.MEMBER)
    }

    fun addOwner(organization: OrganizationEntity, user: UserEntity, addedBy: UserEntity): OrganizationMemberEntity {
        logger.info { "${addedBy.nick} added ${user.nick} to owner of ${organization.name}" }

        return addUser(organization, user, addedBy, OrganizationMemberRole.OWNER)
    }

    @Transactional
    fun removeMember(organization: OrganizationEntity, user: UserEntity) {
        val organizationMember = repository.findByOrganizationIdAndUserId(organization.id!!, user.id!!) ?: return

        if (organizationMember.role == OrganizationMemberRole.OWNER) {
            val owners = getOwnersOfOrganization(organization.id!!)
            if (owners.size <= 1) {
                throw CannotPerformThatActionException("we cannot remove the only owner of the org")
            }
        }

        repository.delete(organizationMember)
    }

    fun assignRole(organization: OrganizationEntity, user: UserEntity, role: OrganizationMemberRole): OrganizationMemberEntity {
        val membership = getOrganizationMember(user, organization)

        if (membership.role == OrganizationMemberRole.OWNER && role == OrganizationMemberRole.MEMBER) {
            val owners = getOwnersOfOrganization(organization.id!!)
            if (owners.size <= 1) {
                throw CannotPerformThatActionException("Organization must have at least one owner")
            }
        }

        membership.role = role
        return repository.save(membership)
    }
    fun decommission(organization: OrganizationEntity) {

        repository.deleteByOrganizationId(organization.id!!)
    }

    private fun addUser(organization: OrganizationEntity, user: UserEntity, addedBy: UserEntity, role: OrganizationMemberRole): OrganizationMemberEntity {
        val organizationMember = OrganizationMemberEntity(
            organization = organization,
            user = user,
            addedBy = addedBy,
            role = role,
            addedAt = OffsetDateTime.now(ZoneOffset.UTC),
        )

        if (getOrganizationMember(user,  organization) != null) {
            throw IllegalArgumentException("User already exists!!!")
        }


        return repository.save(organizationMember)
    }

    fun getOrganizationMember(user: UserEntity, organization: OrganizationEntity): OrganizationMemberEntity {
        return repository.findByOrganizationIdAndUserId(organization.id!!, user.id!!) ?: throw OrganizationMemberNotFoundException(
            user.id!!,
            organization.id!!
        )
    }

    fun getOrganizationsOwnedByUser(userId: Long): List<OrganizationEntity> {
        return repository.findByUserIdAndRole(userId, OrganizationMemberRole.OWNER)
            .map { it.organization }
    }

    fun getOrganizationsAssignedToUser(userId: Long): List<OrganizationEntity> {
        return repository.findByUserIdAndRole(userId, OrganizationMemberRole.MEMBER)
            .map { it.organization }
    }

    fun getMembersOfOrganization(organizationId: Long): List<UserEntity> {
        return repository.findByOrganizationIdAndRole(organizationId, OrganizationMemberRole.MEMBER)
            .map { it.user }
    }

    fun getOwnersOfOrganization(organizationId: Long): List<UserEntity> {
        return repository.findByOrganizationIdAndRole(organizationId, OrganizationMemberRole.OWNER)
            .map { it.user }
    }


}