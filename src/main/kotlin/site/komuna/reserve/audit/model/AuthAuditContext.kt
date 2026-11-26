package site.komuna.reserve.audit.model

data class AuthAuditContext(
    val ipAddress: String?,
    val userAgent: String?,
    val requestId: String?,
) {
    fun toProperties(): Map<String, Any?> = mapOf(
        "ipAddress" to ipAddress,
        "userAgent" to userAgent,
        "requestId" to requestId,
    )
}