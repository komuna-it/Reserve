package site.komuna.reserve.organization

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.komuna.reserve.common.exception.CannotPerformThatActionException
import site.komuna.reserve.common.exception.OrganizationNotFoundException
import site.komuna.reserve.organization.model.CreateOrganizationRequest
import site.komuna.reserve.organization.model.OrganizationDto
import site.komuna.reserve.organization.model.OrganizationEntity
import site.komuna.reserve.organization.model.SearchOrganizationFilter
import site.komuna.reserve.organization.organizationMember.OrganizationMemberRole
import site.komuna.reserve.organization.organizationMember.OrganizationMemberService
import site.komuna.reserve.organization.organizationMember.model.OrganizationMemberDto
import site.komuna.reserve.organization.organizationMember.model.OrganizationMemberEntity
import site.komuna.reserve.user.UserService
import site.komuna.reserve.user.model.UserDto
import site.komuna.reserve.user.model.UserEntity

@Service
class OrganizationService(
    private val repository: OrganizationRepository,
    private val organizationMemberService: OrganizationMemberService,
    private val userService: UserService,
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun getOrganizations(filter: SearchOrganizationFilter, pageable: Pageable): Page<OrganizationDto> {
        val sortedPageable = PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by("name"))

        val targetOrgIds = collectTargetOrganizationIds(filter)

        // return empty page when user was not found
        if ((filter.ownerId != null || filter.userId != null) && targetOrgIds.isEmpty()) {
            return Page.empty(pageable)
        }

        return repository.findAll(specification(filter, targetOrgIds), sortedPageable)
            .map { org ->
                if (!filter.fetchMembers) {
                    OrganizationDto(org)
                } else {
                    OrganizationDto(
                        org,
                        organizationMemberService.getMembersOfOrganization(org.id!!).map { OrganizationMemberDto(it) },
                        organizationMemberService.getOwnersOfOrganization(org.id!!).map { OrganizationMemberDto(it) }
                    )
                }
            }
    }
    // return org when user is a member or an owner
    private fun collectTargetOrganizationIds(filter: SearchOrganizationFilter): Set<Long> {
        val organizationIds = mutableSetOf<Long>()

        filter.ownerId?.let {
            organizationIds += organizationMemberService
                .getOrganizationsOwnedByUser(it)
                .mapNotNull { organization -> organization.id }
        }

        filter.userId?.let {
            organizationIds += organizationMemberService
                .getOrganizationsAssignedToUser(it)
                .mapNotNull { organization -> organization.id }
        }

        return organizationIds
    }

    fun specification(filter: SearchOrganizationFilter, targetOrgIds: Set<Long>) =
        Specification<OrganizationEntity> { root, _, cb ->
            val predicates = mutableListOf<Predicate>()

            filter.organizationId?.let {
                predicates += cb.equal(root.get<Long>("id"), it)
            }

            if (filter.ownerId != null || filter.userId != null) {
                predicates += root.get<Long>("id").`in`(targetOrgIds)
            }

            filter.name?.let {
                predicates += cb.equal(root.get<String>("name"), it)
            }

            cb.and(*predicates.toTypedArray())
        }

    @Transactional
    fun createOrganization(request: CreateOrganizationRequest): OrganizationEntity {
        val user = userService.findById(request.ownerId!!)

        logger.info { "${user.nick} requested to create organization: ${request.name}" }

        val organization = repository.save(
            OrganizationEntity(
                name = request.name,
                created = request.createdAt!!,
                createdBy = user
            )
        )

        organizationMemberService.addOwner(organization, user, user)

        return organization
    }

    @Transactional
    fun addMember(userId: Long, organizationId: Long, addedBy: Long): OrganizationMemberEntity {
        val organization = getOrganization(organizationId)
        val user = userService.findById(userId)
        val addedByUser = userService.findById(addedBy)

        if (!canManageOrganization(addedByUser, organization)) {
            logger.warn { "${addedByUser.nick} tried to add ${user.nick} to ${organization.name} without permissions" }
            throw CannotPerformThatActionException("User does not have permission to add members to this organization")
        }

        return organizationMemberService.addMember(organization, user, addedByUser)
    }

    @Transactional
    fun addOwner(userId: Long, organizationId: Long, addedBy: Long): OrganizationMemberEntity {
        val organization = getOrganization(organizationId)
        val user = userService.findById(userId)
        val addedByUser = userService.findById(addedBy)

        if (!canManageOrganization(addedByUser, organization)) {
            logger.warn { "${addedByUser.nick} tried to add ${user.nick} as owner to ${organization.name} without permissions" }
            throw CannotPerformThatActionException("User does not have permission to add owners to this organization")
        }

        return organizationMemberService.addOwner(organization, user, addedByUser)
    }
    private fun canManageOrganization(user: UserEntity, organization: OrganizationEntity): Boolean {
        return user.role == site.komuna.reserve.user.Role.ADMIN ||
                user.role == site.komuna.reserve.user.Role.MANAGER ||
                isOwner(user, organization)
    }

    fun removeMember(userId: Long, organizationId: Long, removedBy: Long) {
        val organization = getOrganization(organizationId)
        val user = userService.findById(userId)
        val removedByUser = userService.findById(removedBy)

        if (!canManageOrganization(removedByUser, organization)) {
            logger.warn { "${removedByUser.nick} tried to remove ${user.nick} from ${organization.name} without permissions" }
            throw CannotPerformThatActionException("User does not have permission to remove members from this organization")
        }

        if (isOwner(user, organization)) {
            val totalOwnersCount = organizationMemberService.getOwnersOfOrganization(organization.id!!).size
            if (totalOwnersCount <= 1) {
                throw CannotPerformThatActionException("Cannot remove the sole owner of the organization")
            }
        }

        organizationMemberService.removeMember(organization, user)
    }

    @Transactional
    fun assignRole(userId: Long, organizationId: Long, roleStr: String, assignedBy: Long): OrganizationMemberEntity {
        val organization = getOrganization(organizationId)
        val user = userService.findById(userId)
        val assignedByUser = userService.findById(assignedBy)
        val role = OrganizationMemberRole.from(roleStr)

        if (!canManageOrganization(assignedByUser, organization)) {
            throw CannotPerformThatActionException("User does not have permission to assign roles in this organization")
        }

        return organizationMemberService.assignRole(organization, user, role)
    }

    @Transactional
    fun decommission(organizationId: Long, decommissionedBy: Long) {
        val organization = getOrganization(organizationId)
        val decommissionedByUser = userService.findById(decommissionedBy)

        if (!isOwner(decommissionedByUser, organization)) {
            throw CannotPerformThatActionException("User is not an owner of the organization")
        }

        organizationMemberService.decommission(organization)
        repository.delete(organization)
    }

    fun setTrusted(organizationId: Long, trusted: Boolean): OrganizationEntity {
        val organization = getOrganization(organizationId)
        return setTrusted(organization, trusted)
    }

    fun setTrusted(organization: OrganizationEntity, trusted: Boolean): OrganizationEntity {
        organization.trusted = trusted
        return repository.save(organization)
    }

    // ====================================================================================================

    fun isMember(userId: Long, organizationId: Long): Boolean {
        val organization = getOrganization(organizationId)
        val user = userService.findById(userId)

        return isMember(user, organization)
    }

    fun isMember(user: UserEntity, organization: OrganizationEntity): Boolean {
        return organizationMemberService.isUserInOrganization(user.id!!, organization.id!!)
    }

    fun isOwner(user: UserEntity, organization: OrganizationEntity): Boolean {
        val memberships = organizationMemberService.getOrganizationMemberships(user, organization)
        return memberships.any { it.role == OrganizationMemberRole.OWNER }
    }

    fun getOrganization(id: Long): OrganizationEntity {
        return repository.findById(id).orElseThrow { OrganizationNotFoundException(id) }
    }

    fun getAllOrganizationsForUser(userId: Long): List<OrganizationEntity> {
        return organizationMemberService.getAllOrganizationsForUser(userId)
    }
}