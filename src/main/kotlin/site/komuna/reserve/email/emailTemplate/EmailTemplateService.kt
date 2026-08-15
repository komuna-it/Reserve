package site.komuna.reserve.email.emailTemplate

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import site.komuna.reserve.common.exception.ReserveException
import site.komuna.reserve.email.EmailTemplateRepository
import site.komuna.reserve.email.model.EmailTemplate
import site.komuna.reserve.email.model.EmailTemplateEntity
import site.komuna.reserve.email.model.EmailTemplateType
import java.util.concurrent.ConcurrentHashMap

@Service
class EmailTemplateService(
    private val repository: EmailTemplateRepository,
    private val cache: ConcurrentHashMap<String, EmailTemplate> = ConcurrentHashMap<String, EmailTemplate>()
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    /**
     * Search for the template in the database
     */
    fun getTemplate(template: EmailTemplateType, language: String): EmailTemplate {
        val name = getName(template, language)

        if (cache.containsKey(name)) {
            return cache[name]!!
        }

        if( language != "en" ) {
            val englishName = getName(template, "en")
            logger.warn { "Template not found in cache: $name, falling back to english: $englishName" }

            return cache[englishName]!!
        }

        logger.warn { "Template not found in cache: $name" }

        throw Exception("Template not found in cache: $name")
    }

    @PostConstruct
    private fun initialize() {
        logger.info { "Initializing email templates" }
        var count = 0

        repository.findAllForCache().forEach { template ->
            val name = getName(template)

            logger.info { "Caching email template: $name" }

            cache[name] = EmailTemplate(template)
            count++
        }

        logger.info { "Email templates initialized: $count" }
    }

    private fun getName(template: EmailTemplateType, language: String): String {
        return "${template.name}_${language}"
    }

    private fun getName(template: EmailTemplateEntity): String {
        return getName(template.type, template.language)
    }
}