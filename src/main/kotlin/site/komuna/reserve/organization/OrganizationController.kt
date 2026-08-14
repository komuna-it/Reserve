package site.komuna.reserve.organization

import io.github.oshai.kotlinlogging.KotlinLogging
import org.hibernate.validator.internal.util.CollectionHelper.newArrayList
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import site.komuna.reserve.common.PageResponse
import site.komuna.reserve.common.toPageResponse
import site.komuna.reserve.organization.model.CreateOrganizationRequest
import site.komuna.reserve.organization.model.OrganizationDto
import site.komuna.reserve.organization.model.SearchOrganizationFilter
import site.komuna.reserve.organization.model.UpdateTrustedOrganizationStatusRequest
import site.komuna.reserve.organization.organizationMember.model.OrganizationMemberDto
import site.komuna.reserve.user.UserService
import site.komuna.reserve.user.model.UserDto
import java.time.OffsetDateTime
import java.time.ZoneOffset

@RestController
@RequestMapping("/organizations")
class OrganizationController(
    private val service: OrganizationService,
    private val userService: UserService
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @GetMapping("")
    fun getOrganizations(
        @RequestParam(required = false) organizationId: Long?,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) ownerId: Long?,
        @RequestParam(required = false) userId: Long?,
        @RequestParam(defaultValue = "false") fetchMembers: Boolean,
        pageable: Pageable
    ): PageResponse<OrganizationDto> {
        val filter = SearchOrganizationFilter(organizationId, name, ownerId, userId, fetchMembers)
        return service.getOrganizations(filter, pageable).toPageResponse()
    }

    @PostMapping("")
    fun createOrganization(
        @RequestBody request: CreateOrganizationRequest,
        authentication: Authentication
    ): ResponseEntity<OrganizationDto> {
        request.ownerId = authentication.name.toLong()
        request.createdAt = OffsetDateTime.now(ZoneOffset.UTC)

        logger.info { "Received a request from user id ${request.ownerId} to create a new organization: ${request.name}" }

        val organization = service.createOrganization(request)

        val location = ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("/organizations")
            .queryParam("organizationId", organization.id)
            .build()
            .toUri()

        return ResponseEntity
            .created(location)
            .body(OrganizationDto(organization))
    }

    @PostMapping("/addMember/{id}/toOrganization/{organizationId}")
    fun addMemberToOrganization(
        @PathVariable id: Long,
        @PathVariable organizationId: Long,
        authentication: Authentication
    ): ResponseEntity<OrganizationMemberDto> {
        val addedBy = authentication.name.toLong()

        logger.info { "Received a request from user id ${authentication.name} to add user with id: $id to organization with id: $organizationId" }

        val result = service.addMember(id, organizationId, addedBy)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(OrganizationMemberDto(result))
    }

    @PostMapping("/addOwner/{id}/toOrganization/{organizationId}")
    fun addOwnerToOrganization(
        @PathVariable id: Long,
        @PathVariable organizationId: Long,
        authentication: Authentication
    ): ResponseEntity<OrganizationMemberDto> {
        val addedBy = authentication.name.toLong()

        logger.info { "Received a request from user id ${authentication.name} to add user with id: $id as OWNER to organization with id: $organizationId" }

        val result = service.addOwner(id, organizationId, addedBy)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(OrganizationMemberDto(result))
    }

    @PostMapping("/removeMember/{id}/fromOrganization/{organizationId}")
    fun removeMemberFromOrganization(
        @PathVariable id: Long,
        @PathVariable organizationId: Long,
        authentication: Authentication
    ): ResponseEntity<Unit> {
        val removedBy = authentication.name.toLong()
        logger.info { "Received a request from user id ${authentication.name} to remove user with id: $id from organization with id: $organizationId" }

        service.removeMember(id, organizationId, removedBy)

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build()
    }

    @PatchMapping("/assigneUser/{userId}/role/{role}/toOrganization/{organizationId}")
    fun assignRole(
        @PathVariable userId: Long,
        @PathVariable organizationId: Long,
        @PathVariable role: String,
        authentication: Authentication
    ): ResponseEntity<OrganizationMemberDto> {
        val assignedBy = authentication.name.toLong()
        logger.info { "Received a request from user id ${authentication.name} to assign role: $role to user with id: $userId in organization with id: $organizationId" }

        val result = service.assignRole(userId, organizationId, role, assignedBy)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(OrganizationMemberDto(result))
    }

    @DeleteMapping("/decommission/{id}")
    fun decommission(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<Unit> {
        val decommissionedBy = authentication.name.toLong()
        logger.info { "Received a request from user id ${authentication.name} to decommission organization with id: $id" }

        service.decommission(id, decommissionedBy)
        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build()
    }

    @PatchMapping("/trustedStatus")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun setTrusted(
        @RequestBody request: UpdateTrustedOrganizationStatusRequest,
        authentication: Authentication
    ): ResponseEntity<List<OrganizationDto>> {

        val isTrusted = request.trusted

        val organizations = newArrayList<OrganizationDto>()

        request.organizationIds.forEach { organizationId ->
            logger.info { "Received a request from user id ${authentication.name} to set organization with id: $organizationId to trusted: $isTrusted" }
            val organization = service.setTrusted(organizationId, isTrusted)
            organizations.add(OrganizationDto(organization))
        }

        return ResponseEntity.ok(organizations)
    }
}