package soaringLizards.blps.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import soaringLizards.blps.EdxService

@RestController
@RequestMapping("/api/edx")
@Tag(name = "Контроллер прослойка для общения с Tutor")
class EdxController(
    val edxService: EdxService,
) {
    @PostMapping
    fun getAllCourses() =
        ResponseEntity.ok().body(edxService.getCourses())

}
