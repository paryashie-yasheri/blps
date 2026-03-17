package soaringLizards.blps

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["soaringLizards.blps"])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
