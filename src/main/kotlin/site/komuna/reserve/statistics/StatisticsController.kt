package site.komuna.reserve.statistics

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import site.komuna.reserve.statistics.model.DailyStatisticsDto
import site.komuna.reserve.statistics.model.StatisticsCountsDto
import java.time.LocalDate

@RestController
@RequestMapping("/statistics")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
class StatisticsController(
    private val statisticsService: StatisticsService
) {

    @GetMapping("/daily")
    fun getDailyStatistics(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        dateFrom: LocalDate?,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        dateTo: LocalDate?
    ): List<DailyStatisticsDto> {
        return statisticsService.getDailyStatistics(dateFrom, dateTo)
    }

    @GetMapping("/counts")
    fun getCounts(): StatisticsCountsDto {
        return statisticsService.getCounts()
    }
}