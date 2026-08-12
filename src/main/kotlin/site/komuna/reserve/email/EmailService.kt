package site.komuna.reserve.email

import freemarker.template.Configuration
import jakarta.mail.internet.MimeMessage
import org.springframework.core.io.ClassPathResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import freemarker.template.Template
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.scheduling.annotation.Async
import site.komuna.reserve.common.exception.ReserveException
import site.komuna.reserve.email.model.EmailTemplateEntity
import site.komuna.reserve.email.model.EmailTemplateType
import site.komuna.reserve.organization.model.OrganizationEntity
import site.komuna.reserve.organization.organizationMember.OrganizationMemberService
import site.komuna.reserve.settings.SettingsService
import site.komuna.reserve.settings.model.SettingsKey
import site.komuna.reserve.settings.model.SettingsKey.MAIL_SERVER_HOST
import site.komuna.reserve.settings.model.SettingsKey.MAIL_SERVER_PASSWORD
import site.komuna.reserve.settings.model.SettingsKey.MAIL_SERVER_PORT
import site.komuna.reserve.settings.model.SettingsKey.MAIL_SERVER_USERNAME
import site.komuna.reserve.settings.model.SettingsKey.MAIL_SMTP_AUTH
import site.komuna.reserve.settings.model.SettingsKey.MAIL_SMTP_STARTTLS_ENABLE
import site.komuna.reserve.user.UserService
import site.komuna.reserve.user.model.UserEntity
import java.io.StringReader
import java.io.StringWriter

@Service
class EmailService(
    private val freemarkerConfig: Configuration,
    private val organizationMemberService: OrganizationMemberService,
    private val userService: UserService,
    private val emailTemplateRepository: EmailTemplateRepository,
    private val settings: SettingsService,
) {
    @Value("\${spring.profiles.active:beta}")
    private lateinit var activeProfile: String

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    /**
     * Get an email template, render it, and send it
     */
    fun prepareAndSendEmail(type: EmailTemplateType, user: UserEntity, model: MutableMap<String, Any>){
        logger.info { "Sending email ${type.name} to user: ${user.email}" }

        val language = user.preferredLanguage ?: "en"

        model["originalEmail"] = user.email
        model["emailTemplate"] = type.name
        model["emailLanguage"] = language

        val template = getTemplate(type, language)
        val body = render(template, model)

        sendEmail(template.subject, body, user.email, model)
    }

    /**
     * Email all users in an organization
     */
    fun sendEmailToOrganization(type: EmailTemplateType, organization: OrganizationEntity, model: MutableMap<String, Any>){
        logger.info { "Sending email ${type.name} to an organization: ${organization.name}" }

        organizationMemberService.getAllOrganizationUsers(organization.id!!).forEach { member ->
            prepareAndSendEmail(type, member, model)
        }
    }

    /**
     * Find all admins and email each of them
     */
    fun sendEmailToAdmins(type: EmailTemplateType, model: MutableMap<String, Any>) {
        logger.info { "Sending email ${type.name} to admins" }

        userService.getAllAdmins().forEach { admin ->
            prepareAndSendEmail(type, admin, model)
        }
    }

    /**
     * Send an email using the JavaMailSender
     */
    @Async
    public fun sendEmail(subject: String, body: String, recipient: String, model: MutableMap<String, Any>) {
        val sentTo = if(activeProfile != "prod") settings.getStringValue(SettingsKey.MAIL_SERVER_BETA_ADDRESS) else recipient

        try {
            val mailSender = getSender()
            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setFrom(settings.getStringValue(MAIL_SERVER_USERNAME))
            helper.setTo(sentTo)
            helper.setSubject(subject)
            helper.setText(body, true)

            helper.addInline("logo", ClassPathResource("mail/logo.png"))

            mailSender.send(message)
            logger.info { "Email ${model["emailTemplate"]} successfully sent to $sentTo" }
        }
        catch (e: Exception) {
            logger.error { "Email ${model["emailTemplate"]} was not sent to $sentTo" }
            e.printStackTrace()
        }
    }

    /**
     * Build a sender config based on cached settings
     */
    private fun getSender(): JavaMailSender{
        val sender = JavaMailSenderImpl()

        sender.host = settings.getStringValue(MAIL_SERVER_HOST)
        sender.port = settings.getIntValue(MAIL_SERVER_PORT)
        sender.username = settings.getStringValue(MAIL_SERVER_USERNAME)
        sender.password = settings.getStringValue(MAIL_SERVER_PASSWORD)
        sender.javaMailProperties["mail.smtp.auth"] = settings.getBooleanValue(MAIL_SMTP_AUTH)
        sender.javaMailProperties["mail.smtp.starttls.enable"] = settings.getBooleanValue(MAIL_SMTP_STARTTLS_ENABLE)

        return sender
    }

    /**
     * Search for the template in the database
     */
    private fun getTemplate(template: EmailTemplateType, language: String): EmailTemplateEntity {
        var emailTemplate = emailTemplateRepository.findByTypeAndLanguage(template, language)

        if (emailTemplate == null) {
            emailTemplate = emailTemplateRepository.findByTypeAndLanguage(template, "en")
        }

        if (emailTemplate == null) {
            throw ReserveException(HttpStatus.NOT_FOUND, "Email template not found")
        }

        return emailTemplate
    }

    /**
     * Render the email body with provided data in model
     */
    private fun render(template: EmailTemplateEntity, model: Map<String, Any>): String {
        val header = template.header?.fragment ?: ""
        val body = template.body.fragment
        val footer = template.footer?.fragment ?: ""

        val debugInfo = if (activeProfile != "prod") {
            """
            <p style="color: red; font-size: 12px;"> EMAIL REDIRECTED TO BETA MAILBOX </p>
            <p> Original email: ${model["originalEmail"]} </p>
            <p> email template: ${model["emailTemplate"]} </p>
            <p> email language: ${model["emailLanguage"]} </p>
            <hr/>
            <br/>
        """.trimIndent()
        } else {
            ""
        }

        val htmlBody = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="color-scheme" content="light">
                <meta name="supported-color-schemes" content="light">
            </head>
            <body style="margin: 0; padding: 20px; background-color: #eeeeee;">
                $debugInfo
                <table width="100%" cellpadding="0" cellspacing="0" border="0">
                    <tr>
                        <td align="center">
                            <table width="600" cellpadding="0" cellspacing="0" border="0" style=" background-color: #ffffff; border-collapse: collapse;">
                                $header
                                $body
                                $footer
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
        """.trimIndent()

        val template = Template("email", StringReader(htmlBody), freemarkerConfig)
        val writer = StringWriter()

        template.process(model, writer)

        return writer.toString()
    }
}