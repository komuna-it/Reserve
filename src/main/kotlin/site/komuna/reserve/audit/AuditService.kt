package site.komuna.reserve.audit


import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import site.komuna.reserve.audit.model.AuditEntity
import site.komuna.reserve.audit.model.AuditType
import site.komuna.reserve.user.model.UserEntity

@Service
class AuditService(
    private val auditRepository: AuditRepository
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun log(
        type: AuditType,
        actor: UserEntity? = null,
        properties: Map<String, Any?> = emptyMap()
    ): AuditEntity {
        return auditRepository.save(
            AuditEntity(
                type = type,
                actor = actor,
                properties = properties
            )
        )
    }
}