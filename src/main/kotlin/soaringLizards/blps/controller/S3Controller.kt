package soaringLizards.blps.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import soaringLizards.blps.service.S3Service

@RestController
@RequestMapping("/api/s3")
@Tag(name = "S3")
class S3Controller(
    private val s3Service: S3Service,
) {
    @GetMapping("/{key}")
    fun get(@PathVariable("key") key: String) = s3Service.get(key)

    @PostMapping(consumes = ["multipart/form-data"])
    fun upload(@RequestParam file: MultipartFile) = s3Service.upload(file)!!

    @DeleteMapping("/{key}")
    fun delete(@PathVariable("key") key: String) = s3Service.delete(key)

}
