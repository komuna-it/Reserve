package site.komuna.reserve.audit

import org.springframework.data.jpa.repository.JpaRepository
import site.komuna.reserve.audit.model.AuditEntity

interface AuditRepository : JpaRepository<AuditEntity, Long>