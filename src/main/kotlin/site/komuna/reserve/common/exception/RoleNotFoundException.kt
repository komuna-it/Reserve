package site.komuna.reserve.common.exception

import org.springframework.http.HttpStatus

class RoleNotFoundException(name: String): ReserveException(HttpStatus.NOT_FOUND, "Role $name not found") {
}