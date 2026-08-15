package site.komuna.reserve.email.model

class EmailTemplate(
    private val rawSubject: String,
    private val rawHeader: String? = null,
    private val rawBody: String,
    private val rawFooter: String? = null,
) {
    constructor(emailTemplateEntity: EmailTemplateEntity) : this(
        rawSubject = emailTemplateEntity.subject,
        rawHeader = emailTemplateEntity.header?.fragment,
        rawBody = emailTemplateEntity.body.fragment,
        rawFooter = emailTemplateEntity.footer?.fragment
    )

    val subject: String
        get() = rawSubject

    val header: String
        get() = rawHeader.orEmpty()

    val body: String
        get() = rawBody

    val footer: String
        get() = rawFooter.orEmpty()
}