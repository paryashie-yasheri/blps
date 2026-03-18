package soaringLizards.blps.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import soaringLizards.blps.EdxService
import soaringLizards.blps.edx.model.Course

@RestController
@RequestMapping("edx")
@Tag(name = "Edx")
class EdxController(
    val edxService: EdxService,
) {
    @PostMapping()
    fun getAllCources(@RequestBody data: Course) =
        ResponseEntity.ok().body(edxService.getCoursesApi(data))

}