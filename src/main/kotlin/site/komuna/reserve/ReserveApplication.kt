package site.komuna.reserve

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class ReserveApplication

fun main(args: Array<String>) {
	runApplication<ReserveApplication>(*args)
}
