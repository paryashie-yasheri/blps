package soaringLizards.blps.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@RestController
class HelloWorld {
    @GetMapping("/hello")
    fun hi(): String {
        return "hello"
    }
}

