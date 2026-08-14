package site.komuna.reserve.email.model

import site.komuna.reserve.user.model.UserEntity

class EmailRecipient(
    val email: String,
    val nick: String,
    val language: String,
) {
    constructor(user: UserEntity) : this(user.email, user.nick, user.preferredLanguage)  {
    }
}