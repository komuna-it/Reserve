package site.komuna.reserve.organization

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import site.komuna.reserve.organization.model.CreateOrganizationRequest
import site.komuna.reserve.organization.model.OrganizationDto
import site.komuna.reserve.organization.model.SearchOrganizationFilter
import site.komuna.reserve.organization.organizationMember.model.OrganizationMemberDto
import site.komuna.reserve.user.model.UserDto
import java.time.OffsetDateTime
import java.time.ZoneOffset

@RestController
@RequestMapping("/organizations")
class OrganizationController(
    private val service: OrganizationService,
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
    ): Page<OrganizationDto> {

        val filter = SearchOrganizationFilter(organizationId, name, ownerId, userId, fetchMembers)

        return service.getOrganizations(filter, pageable)
    }

    @PostMapping("")
    fun createOrganization(@RequestBody request: CreateOrganizationRequest, authentication: Authentication): ResponseEntity<OrganizationDto> {
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
    fun addMemberToOrganization(@PathVariable id: Long, @PathVariable organizationId: Long, authentication: Authentication): ResponseEntity<OrganizationMemberDto> {
        val addedBy = authentication.name.toLong()

        logger.info { "Received a request from user id ${authentication.name} to add user with id: $id to organization with id: $organizationId" }

        val result = service.addMember(id, organizationId, addedBy)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(OrganizationMemberDto(result))
    }

    @PostMapping("/removeMember/{id}/fromOrganization/{organizationId}")
    fun removeMemberFromOrganization(@PathVariable id: Long, @PathVariable organizationId: Long, authentication: Authentication): ResponseEntity<Unit> {
        val removedBy = authentication.name.toLong()
        logger.info { "Received a request from user id ${authentication.name} to remove user with id: $id from organization with id: $organizationId" }

        service.removeMember(id, organizationId, removedBy)

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build()
    }

    @PatchMapping("/assigneUser/{userId}/role/{role}/toOrganization/{organizationId}")
    fun assignRole(@PathVariable userId: Long, @PathVariable organizationId: Long, @PathVariable role: String, authentication: Authentication): ResponseEntity<OrganizationMemberDto> {
        val assignedBy = authentication.name.toLong()
        logger.info { "Received a request from user id ${authentication.name} to assign role: $role to user with id: $userId in organization with id: $organizationId" }

        val result = service.assignRole(userId, organizationId, role, assignedBy)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(OrganizationMemberDto(result))
    }

    @DeleteMapping("/decommission/{id}")
    fun decommission(@PathVariable id: Long, authentication: Authentication): ResponseEntity<Unit> {
        val decommissionedBy = authentication.name.toLong()
        logger.info { "Received a request from user id ${authentication.name} to decommission organization with id: $id" }

        service.decommission(id, decommissionedBy)
        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build()
    }


    @PatchMapping("/{organizationId}/isTrusted/{isTrusted}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun setTrusted(@PathVariable organizationId: Long, @PathVariable isTrusted: Boolean, authentication: Authentication): ResponseEntity<OrganizationDto> {
        logger.info { "Received a request from user id ${authentication.name} to set organization with id: $organizationId to trusted: $isTrusted" }

        val organization = service.setTrusted(organizationId, isTrusted)
        return ResponseEntity.ok(OrganizationDto(organization))
    }
}