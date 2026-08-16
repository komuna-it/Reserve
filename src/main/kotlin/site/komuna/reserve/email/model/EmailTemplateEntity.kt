package site.komuna.reserve.email.model

import jakarta.persistence.*

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