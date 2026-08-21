package site.komuna.reserve.user

import site.komuna.reserve.common.httpError.exception.RoleNotFoundException

enum class Role {
    ORPHAN,
    USER,
    MANAGER,
    ADMIN,
    SYSTEM; // SYSTEM is a special role used for internal purposes. This role should not be used by users.

    companion object {
        fun from(value: String): Role {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true)
            } ?: throw RoleNotFoundException(value)
        }
    }
}