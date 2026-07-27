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
        prepareSearch(filter)

        val sortedPageable = PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by("name"))

        return repository.findAll(specification(filter), sortedPageable)
            .map { org ->
                if (!filter.fetchMembers) {
                    OrganizationDto(org)
                } else {
                    OrganizationDto(
                        org,
                        organizationMemberService.getMembersOfOrganization(org.id!!).map { UserDto(it) },
                        organizationMemberService.getOwnersOfOrganization(org.id!!).map { UserDto(it) }
                    )
                }
            }
    }

    fun specification(filter: SearchOrganizationFilter) =
        Specification<OrganizationEntity> { root, _, cb ->
            val predicates = mutableListOf<Predicate>()

            filter.organizationId?.let {
                predicates += cb.equal(root.get<Long>("id"), it)
            }

            if (filter.ownerId != null || filter.userId != null) {
                predicates += root.get<Long>("id").`in`(filter.organizationsIds)
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

    fun addMember(userId: Long, organizationId: Long, addedBy: Long): OrganizationMemberEntity {
        val organization = getOrganization(organizationId)
        val user = userService.findById(userId)
        val addedByUser = userService.findById(addedBy)

        if (!isMember(addedByUser, organization)) {
            logger.warn { "${addedByUser.nick} tried to add ${user.nick} to ${organization.name} but is not a member" }
            throw CannotPerformThatActionException("User is not a member of the organization")
        }

        return organizationMemberService.addMember(organization, user, addedByUser)
    }


    fun removeMember(userId: Long, organizationId: Long, removedBy: Long) {
        val organization = getOrganization(organizationId)
        val user = userService.findById(userId)
        val removedByUser = userService.findById(removedBy)

        if (!isOwner(removedByUser, organization)) {
            logger.warn { "${removedByUser.nick} tried to remove ${user.nick} from ${organization.name} without being an owner" }
            throw CannotPerformThatActionException("User is not an owner of the organization")
        }

        if (isOwner(user, organization)) {
            val totalOwnersCount = organizationMemberService.getOwnersOfOrganization(organization.id!!).size
            if (totalOwnersCount <= 1) {
                throw CannotPerformThatActionException("Cannot remove the sole owner of the organization")
            }
        }

        organizationMemberService.removeMember(organization, user)
    }
    fun assignRole(userId: Long, organizationId: Long, roleStr: String, assignedBy: Long): OrganizationMemberEntity {
        val organization = getOrganization(organizationId)
        val user = userService.findById(userId)
        val assignedByUser = userService.findById(assignedBy)
        val role = OrganizationMemberRole.from(roleStr)

        if (!isOwner(assignedByUser, organization)) {
            throw CannotPerformThatActionException("User is not an owner of the organization")
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

    // ====================================================================================================

    private fun prepareSearch(filter: SearchOrganizationFilter): SearchOrganizationFilter {
        val organizationIds = mutableSetOf<Long>()

        filter.ownerId?.let {
            organizationIds += organizationMemberService
                .getOrganizationsOwnedByUser(it)
                .map { organization -> organization.id!! }
        }

        filter.userId?.let {
            organizationIds += organizationMemberService
                .getOrganizationsAssignedToUser(it)
                .map { organization -> organization.id!! }
        }

        filter.organizationsIds.addAll(organizationIds)
        return filter
    }

    fun getAllOrganizationsForUser(userId: Long): List<OrganizationEntity> {
        return organizationMemberService.getAllOrganizationsForUser(userId)
    }
}