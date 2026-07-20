package site.komuna.reserve.user

import site.komuna.reserve.common.exception.RoleNotFoundException

enum class Role {
    USER,
    MANAGER,
    ADMIN,
    SYSTEM; // SYSTEM is a special role used for internal purposes. It is not exposed to the user.

    companion object {
        fun from(value: String): Role {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true)
            } ?: throw RoleNotFoundException(value)
        }
    }
}