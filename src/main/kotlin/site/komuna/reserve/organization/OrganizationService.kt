package site.komuna.reserve.organization

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import site.komuna.reserve.common.exception.CannotPerformThatActionException
import site.komuna.reserve.common.exception.OrganizationNotFoundException
import site.komuna.reserve.organization.model.CreateOrganizationRequest
import site.komuna.reserve.organization.model.OrganizationDto
import site.komuna.reserve.organization.model.OrganizationEntity
import site.komuna.reserve.organization.model.SearchOrganizationFilter
import site.komuna.reserve.organization.organizationMember.OrganizationMemberRole
import site.komuna.reserve.organization.organizationMember.OrganizationMemberService
import site.komuna.reserve.organization.organizationMember.model.OrganizationMemberEntity
import site.komuna.reserve.organization.repository.OrganizationRepository
import site.komuna.reserve.organization.repository.OrganizationSearchRepository
import site.komuna.reserve.user.UserService
import site.komuna.reserve.user.model.UserDto
import site.komuna.reserve.user.model.UserEntity

@Service
class OrganizationService(
    private val repository: OrganizationRepository,
    private val searchRepository: OrganizationSearchRepository,
    private val organizationMemberService: OrganizationMemberService,
    private val userService: UserService,
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun getOrganizations(filter: SearchOrganizationFilter, pageable: Pageable): Page<OrganizationDto> {
        prepareSearch(filter)
        return searchRepository.search(filter, pageable)
            .map { organization ->
                if (!filter.fetchMembers) {
                    OrganizationDto(organization)
                } else {
                    val members =
                        organizationMemberService.getMembersOfOrganization(organization.id!!)
                            .map { UserDto(it) }
                    val owners =
                        organizationMemberService.getOwnersOfOrganization(organization.id!!)
                            .map { UserDto(it) }

                    OrganizationDto(
                        organization,
                        members,
                        owners
                    )
                }
            }
    }

    fun createOrganization(request: CreateOrganizationRequest): OrganizationEntity {
        val user = userService.findById(request.ownerId!!)

        logger.info { "${user.nick} requested to create organization: ${request.name}" }

        val organization = repository.save(
            OrganizationEntity(
                name = request.name,
                created = request.createdAt!!,
                createdBy = user)
        )

        organizationMemberService.addOwner(organization, user, user)
        return organization
    }

    fun addMember(userId: Long, organizationId: Long, addedBy: Long) : OrganizationMemberEntity {
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

        // Maybe we could remove the owner when there are more than one owner.
        // Current process allows us to change a role of members so we have a workaround
        if(isOwner(user, organization)) {
            throw CannotPerformThatActionException("Cannot remove an owner from the organization")
        }

        organizationMemberService.removeMember(organization, user)
    }

    fun assignRole(userId: Long, organizationId: Long, role: String, assignedBy: Long): OrganizationMemberEntity {
        val organization = getOrganization(organizationId)
        val user = userService.findById(userId)
        val assignedByUser = userService.findById(assignedBy)
        val role = OrganizationMemberRole.from(role)

        if(!isOwner(assignedByUser, organization)) {
            throw CannotPerformThatActionException("User is not an owner of the organization")
        }

        return organizationMemberService.assignRole(organization, user, role)
    }

    @Transactional
    fun decommission(organizationId: Long, decommissionedBy: Long) {
        val organization = getOrganization(organizationId)
        val decommissionedByUser = userService.findById(decommissionedBy)

        if(!isOwner(decommissionedByUser, organization)) {
            throw CannotPerformThatActionException("User is not an owner of the organization")
        }

        organizationMemberService.decommission(organization)
        repository.delete(organization)
    }

    // ====================================================================================================
    fun isMember(userId: Long, organizationId: Long): Boolean {
        val organization = getOrganization(organizationId)
        val user = userService.findById(userId)

        return isMember(user, organization)
    }

    fun isMember(user: UserEntity, organization: OrganizationEntity): Boolean {
        val membership = organizationMemberService.getOrganizationMember(user, organization)
        return true
    }

    fun isOwner(user: UserEntity, organization: OrganizationEntity): Boolean {
        val membership = organizationMemberService.getOrganizationMember(user, organization)
        return membership.role == OrganizationMemberRole.OWNER
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
}