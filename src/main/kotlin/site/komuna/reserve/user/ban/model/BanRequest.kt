package site.komuna.reserve.user.ban.model

import java.time.Duration

class BanRequest(
    var userId: Long,
    var reason: String,
    var duration: Duration
) {
}