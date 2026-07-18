package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class OrganizationMemberNotFoundException(userId: Long, organizationId: Long): ReserveException(HttpStatus.NOT_FOUND, "User with id: $userId is not member of organization with id: $organizationId") {
}