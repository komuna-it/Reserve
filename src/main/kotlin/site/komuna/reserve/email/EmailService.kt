package site.komuna.reserve.email

import freemarker.template.Configuration
import freemarker.template.Template
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.mail.internet.MimeMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import site.komuna.reserve.email.emailTemplate.EmailTemplateService
import site.komuna.reserve.email.model.EmailRecipient
import site.komuna.reserve.email.model.EmailTemplate
import site.komuna.reserve.email.model.EmailTemplateType
import site.komuna.reserve.settings.SettingsService
import site.komuna.reserve.settings.model.SettingsKey
import site.komuna.reserve.settings.model.SettingsKey.*
import java.io.StringReader
import java.io.StringWriter

@Service
class EmailService(
    private val freemarkerConfig: Configuration,
    private val settings: SettingsService,
    private val emailTemplateService: EmailTemplateService
) {
    @Value("\${spring.profiles.active:beta}")
    private lateinit var activeProfile: String

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    /**
     * Get an email template, render it, and send it
     */
    @Async
    fun sendEmailToUser(type: EmailTemplateType, recipient: EmailRecipient, model: MutableMap<String, Any>){
        logger.info { "Sending email ${type.name} to user: ${recipient.email}" }

        sendEmail(type, recipient, model)
    }

    /**
     * Email list of users
     */
    @Async
    fun sendEmailToUsers(type: EmailTemplateType, recipients: List<EmailRecipient>, model: MutableMap<String, Any>){
        logger.info { "Sending email ${type.name} to ${recipients.size} users" }

        recipients.forEach { recipient ->
            sendEmailToUser(type, recipient, model)
        }
    }

    /**
     * Send an email using the JavaMailSender
     */
    private fun sendEmail(type: EmailTemplateType, recipient: EmailRecipient, model: MutableMap<String, Any>) {
        val sentFrom = settings.getStringValue(MAIL_SERVER_USERNAME)
        val sentTo = if(activeProfile != "prod") settings.getStringValue(SettingsKey.MAIL_SERVER_BETA_ADDRESS) else recipient.email

        try {
            val language = recipient.language

            val template = emailTemplateService.getTemplate(type, language)
            val subject = template.subject

            model["originalEmail"] = recipient.email
            model["emailTemplate"] = type.name
            model["emailLanguage"] = language
            model["nick"] = recipient.nick
            val body = render(template, model)

            val mailSender = getSender()
            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setFrom(sentFrom)
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
     * Render the email body with provided data in a model
     */
    private fun render(template: EmailTemplate, model: Map<String, Any>): String {
        val header = template.header
        val body = template.body
        val footer = template.footer

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