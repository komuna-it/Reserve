package site.komuna.reserve.email.model

import jakarta.persistence.*

@Entity
@Table(name = "email_fragments")
class EmailFragmentEntity(

    @Id
    var name: String? = null,

    @Column(columnDefinition = "TEXT")
    var fragment: String,

    @Enumerated(EnumType.STRING)
    var type: EmailFragmentType,
) {
}