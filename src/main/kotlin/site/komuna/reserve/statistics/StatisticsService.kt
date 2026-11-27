package site.komuna.reserve.statistics

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.komuna.reserve.organization.OrganizationRepository
import site.komuna.reserve.reservation.model.ReservationStatus
import site.komuna.reserve.room.pricing.PricingService
import site.komuna.reserve.statistics.model.DailyStatisticsDto
import site.komuna.reserve.statistics.model.RoomDailyStatisticsDto
import site.komuna.reserve.statistics.model.StatisticsCountsDto
import site.komuna.reserve.user.Role
import site.komuna.reserve.user.UserRepository
import java.time.LocalDate
import java.time.ZoneId

@Service
class StatisticsService(
    private val repository: StatisticsRepository,
    private val pricingService: PricingService,
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository

) {
    companion object {
        private val ZONE_ID = ZoneId.of("Europe/Warsaw")
    }

    @Transactional(readOnly = true)
    fun getCounts(): StatisticsCountsDto {
        return StatisticsCountsDto(
            totalUsers = userRepository.countByRole(Role.USER),
            totalOrganizations = organizationRepository.count()
        )
    }

    @Transactional(readOnly = true)
    fun getDailyStatistics( dateFrom: LocalDate?, dateTo: LocalDate?): List<DailyStatisticsDto> {
        val today = LocalDate.now(ZONE_ID)
        val from = dateFrom ?: today.withDayOfMonth(1)
        val to = dateTo ?: today.withDayOfMonth(today.lengthOfMonth())

        require(!from.isAfter(to)) {
            "dateFrom cannot be after dateTo"
        }

        val startAt = from.atStartOfDay(ZONE_ID).toOffsetDateTime()
        val endAt = to.plusDays(1).atStartOfDay(ZONE_ID).toOffsetDateTime()

        val reservations = repository
            .findAllByStartAtGreaterThanEqualAndStartAtLessThan(startAt, endAt)

        val grouped = reservations.groupBy {
            it.startAt.atZoneSameInstant(ZONE_ID).toLocalDate()
        }

        return generateSequence(from) { current ->
            if (current.isBefore(to)) current.plusDays(1) else null
        }.map { date ->
            val dailyReservations = grouped[date].orEmpty()

            val roomStatistics = dailyReservations
                .groupBy { it.room.id!! }
                .map { (roomId, roomReservations) ->

                    val paidEarnings = roomReservations
                        .asSequence()
                        .filter {
                            it.status == ReservationStatus.CONFIRMED && it.paid
                        }
                        .sumOf {
                            pricingService.getPrice(it).toLong()
                        }

                    val unpaidEarnings = roomReservations
                        .asSequence()
                        .filter {
                            it.status == ReservationStatus.CONFIRMED && !it.paid
                        }
                        .sumOf {
                            pricingService.getPrice(it).toLong()
                        }

                    RoomDailyStatisticsDto(
                        roomId = roomId,
                        roomName = roomReservations.first().room.name,
                        reservations = roomReservations.size.toLong(),
                        earnings = paidEarnings,
                        unpaidEarnings = unpaidEarnings
                    )
                }
                .sortedBy { it.roomName }

            val earnings = roomStatistics.sumOf { it.earnings }
            val unpaidEarnings = roomStatistics.sumOf { it.unpaidEarnings }

            DailyStatisticsDto(
                date = date,
                rooms = roomStatistics,
                total = dailyReservations.size.toLong(),
                earnings = earnings,
                unpaidEarnings = unpaidEarnings
            )
        }.toList()
    }
}