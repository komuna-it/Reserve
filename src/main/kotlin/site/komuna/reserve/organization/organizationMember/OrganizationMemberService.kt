package site.komuna.reserve.organization.organizationMember

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import site.komuna.reserve.common.httpError.exception.CannotPerformThatActionException
import site.komuna.reserve.common.httpError.exception.OrganizationLastOwnerException
import site.komuna.reserve.common.httpError.exception.OrganizationMemberNotFoundException
import site.komuna.reserve.common.httpError.exception.UserIsMemberOfOrganizationException
import site.komuna.reserve.organization.model.OrganizationEntity
import site.komuna.reserve.organization.organizationMember.model.OrganizationMemberEntity
import site.komuna.reserve.user.Role
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

    fun isMember(userId: Long, organizationId: Long): Boolean {
        return repository.existsByOrganizationIdAndUserId(organizationId, userId)
    }

    fun addMember(organization: OrganizationEntity, user: UserEntity, addedBy: UserEntity): OrganizationMemberEntity {
        logger.info { "${addedBy.nick} added ${user.nick} to ${organization.name}" }
        return addUser(organization, user, addedBy, OrganizationMemberRole.MEMBER)
    }

    fun addOwner(organization: OrganizationEntity, user: UserEntity, addedBy: UserEntity): OrganizationMemberEntity {
        logger.info { "${addedBy.nick} added ${user.nick} as OWNER of ${organization.name}" }
        return addUser(organization, user, addedBy, OrganizationMemberRole.OWNER)
    }

    fun getOrganizationMemberships(user: UserEntity, organization: OrganizationEntity): List<OrganizationMemberEntity> {
        return repository.findAllByOrganizationIdAndUserId(organization.id!!, user.id!!)
    }

    @Transactional
    fun removeMember(organization: OrganizationEntity, user: UserEntity) {
        val memberships = repository.findAllByOrganizationIdAndUserId(organization.id!!, user.id!!)

        if (memberships.isEmpty()) {
            return
        }

        val isOwner = memberships.any { it.role == OrganizationMemberRole.OWNER }

        if (isOwner) {
            val totalOwnersCount = repository.countByOrganizationIdAndRole(organization.id!!, OrganizationMemberRole.OWNER)

            if (totalOwnersCount <= 1) {
                throw CannotPerformThatActionException("cannot remove the only owner")
            }
        }

        repository.deleteAll(memberships)
    }

    fun unassignOrphanUsers() {
        repository.deleteByUserRole(Role.ORPHAN)
    }

    @Transactional
    fun assignRole(organization: OrganizationEntity, user: UserEntity, role: OrganizationMemberRole): OrganizationMemberEntity {
        val memberships = repository.findAllByOrganizationIdAndUserId(organization.id!!, user.id!!)

        if (memberships.isEmpty()) {
            return addUser(organization, user, user, role)
        }

        if (memberships.any { it.role == OrganizationMemberRole.OWNER } && role == OrganizationMemberRole.MEMBER) {
            val owners = getOwnersOfOrganization(organization.id!!)
            if (owners.size <= 1) {
                throw OrganizationLastOwnerException()
            }
        }

        if (memberships.size > 1) {
            repository.deleteAll(memberships.drop(1))
        }

        val primaryMembership = memberships.first()
        primaryMembership.role = role
        return repository.save(primaryMembership)
    }

    @Transactional
    fun decommission(organization: OrganizationEntity) {
        repository.deleteByOrganizationId(organization.id!!)
    }

    private fun addUser(organization: OrganizationEntity, user: UserEntity, addedBy: UserEntity, role: OrganizationMemberRole): OrganizationMemberEntity {
        if (isUserInOrganization(user.id!!, organization.id!!)) {
            throw UserIsMemberOfOrganizationException()
        }
        val organizationMember = OrganizationMemberEntity(
            organization = organization,
            user = user,
            addedBy = addedBy,
            role = role,
            addedAt = OffsetDateTime.now(ZoneOffset.UTC),
        )

        return repository.save(organizationMember)
    }

    fun isUserInOrganization(userId: Long, organizationId: Long): Boolean {
        return repository.existsByOrganizationIdAndUserId(organizationId, userId)
    }

    fun getOrganizationMember(user: UserEntity, organization: OrganizationEntity): OrganizationMemberEntity {
        return getOrganizationMemberOrNull(user, organization)
            ?: throw OrganizationMemberNotFoundException(user.id!!, organization.id!!)
    }

    fun isOwnerOrAdmin(user: UserEntity, organization: OrganizationEntity): Boolean {
        if (user.role == Role.ADMIN) {
            return true
        }
        return isOwner(user, organization)
    }

    fun isOwner(user: UserEntity, organization: OrganizationEntity): Boolean {
        val memberships = repository.findAllByOrganizationIdAndUserId(organization.id!!, user.id!!)
        return memberships.any { it.role == OrganizationMemberRole.OWNER }
    }

    fun getOrganizationMemberOrNull(user: UserEntity, organization: OrganizationEntity): OrganizationMemberEntity? {
        return repository.findAllByOrganizationIdAndUserId(organization.id!!, user.id!!).firstOrNull()
    }

    fun getOrganizationsOwnedByUser(userId: Long): List<OrganizationEntity> {
        return repository.findByUserIdAndRole(userId, OrganizationMemberRole.OWNER)
            .map { it.organization }
    }

    fun getOrganizationsAssignedToUser(userId: Long): List<OrganizationEntity> {
        return repository.findByUserId(userId)
            .map { it.organization }
            .distinctBy { it.id }
    }

    fun getMembersOfOrganization(organizationId: Long): List<OrganizationMemberEntity> {
        return repository.findByOrganizationIdAndRole(organizationId, OrganizationMemberRole.MEMBER)
    }

    fun getOwnersOfOrganization(organizationId: Long): List<OrganizationMemberEntity> {
        return repository.findByOrganizationIdAndRole(organizationId, OrganizationMemberRole.OWNER)
    }

    fun getAllOrganizationUsers(organizationId: Long): List<UserEntity> {
        val members = getMembersOfOrganization(organizationId)
            .map { it.user }
        val owners = getOwnersOfOrganization(organizationId)
            .map { it.user }
        return members + owners
    }

    fun getAllOrganizationsForUser(userId: Long): List<OrganizationEntity> {
        return repository.findByUserId(userId)
            .map { it.organization }
            .distinctBy { it.id }
    }


}