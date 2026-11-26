package site.komuna.reserve.audit.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

import site.komuna.reserve.user.model.UserEntity

@Entity
@Table(name = "audit")
class AuditEntity (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = true)
    var actor: UserEntity? = null,

    @Enumerated(EnumType.STRING)
    var type: AuditType,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var properties: Map<String, Any?> = emptyMap(),

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()
    )
