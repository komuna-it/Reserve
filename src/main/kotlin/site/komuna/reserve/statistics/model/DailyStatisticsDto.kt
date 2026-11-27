package site.komuna.reserve.statistics.model

import java.time.LocalDate

data class DailyStatisticsDto(
    val date: LocalDate,
    val rooms: List<RoomDailyStatisticsDto>,
    val total: Long,
    val earnings: Long,
    val unpaidEarnings: Long
)

data class RoomDailyStatisticsDto(
    val roomId: Long,
    val roomName: String,
    val reservations: Long,
    val earnings: Long,
    val unpaidEarnings: Long
)