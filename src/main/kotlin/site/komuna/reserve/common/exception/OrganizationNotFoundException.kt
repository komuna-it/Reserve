package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class OrganizationNotFoundException(id: Long): ReserveException(HttpStatus.NOT_FOUND, "Organization with id: $id not found") {
}