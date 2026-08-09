package site.komuna.reserve.email

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import site.komuna.reserve.email.model.EmailTemplateEntity
import site.komuna.reserve.email.model.EmailTemplateType

@Repository
interface EmailTemplateRepository: JpaRepository<EmailTemplateEntity, Long> {

    fun findByTypeAndLanguage(type: EmailTemplateType, language: String): EmailTemplateEntity?
}