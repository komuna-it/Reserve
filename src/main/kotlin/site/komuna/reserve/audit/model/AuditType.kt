package site.komuna.reserve.audit.model

import site.komuna.reserve.common.httpError.exception.AuditTypeNotFoundException

enum class AuditType {
    USER_REGISTER_SUCCESS,
    USER_REGISTER_FAILED,
    USER_DELETED,
    USER_LOGIN_SUCCESS,
    USER_LOGIN_FAILED,
    USER_LOGOUT,
    USER_SESSION_REFRESHED,
    USER_BANNED,
    USER_UNBANNED,
    RESERVATION_CREATED,
    RESERVATION_STATUS_UPDATED,
    RESERVATION_PAID_UPDATED,
    ORGANIZATION_CREATED,
    ORGANIZATION_MEMBERS_UPDATE,
    ROOM_UPDATED,
    PASSWORD_RESET,
    PRICE_UPDATED,
    EMAIL_SENT_TO_USERS,
    EMAIL_SENT_TO_MANAGERS,
    ERROR,
    USER_LOGIN_BLOCKED,
    USER_SESSION_REFRESH_FAILED,
    USER_EMAIL_VERIFIED,
    USER_EMAIL_VERIFICATION_FAILED,;

    companion object {
        fun from(value: String): AuditType {
            return AuditType.entries.firstOrNull {
                it.name.equals(value, ignoreCase = true)
            } ?: throw AuditTypeNotFoundException(value)
        }
    }
}