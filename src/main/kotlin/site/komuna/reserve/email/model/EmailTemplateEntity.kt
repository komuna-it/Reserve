package site.komuna.reserve.email.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "email_templates")
class EmailTemplateEntity(
    @Id
    var name: String? = null,

    @Enumerated(EnumType.STRING)
    var type: EmailTemplateType,

    @Column(length = 2)
    var language: String,

    var subject: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "header_id", nullable = true)
    var header: EmailFragmentEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "body_id", nullable = false)
    var body: EmailFragmentEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "footer_id", nullable = true)
    var footer: EmailFragmentEntity? = null,
) {


}