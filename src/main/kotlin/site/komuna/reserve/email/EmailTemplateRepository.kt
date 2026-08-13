package site.komuna.reserve.email

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import site.komuna.reserve.email.model.EmailTemplateEntity
import site.komuna.reserve.email.model.EmailTemplateType

@Repository
interface EmailTemplateRepository: JpaRepository<EmailTemplateEntity, Long> {

    @Query("""
    select t
    from EmailTemplateEntity t
    left join fetch t.header
    join fetch t.body
    left join fetch t.footer
""")
    fun findAllForCache(): List<EmailTemplateEntity>

    fun findByTypeAndLanguage(type: EmailTemplateType, language: String): EmailTemplateEntity?
}