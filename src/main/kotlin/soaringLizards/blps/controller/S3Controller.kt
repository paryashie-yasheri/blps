package soaringLizards.blps.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
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
@RequestMapping("/api/img/s3")
@Tag(name = "S3")
class S3Controller(
    private val s3Service: S3Service,
) {
    @GetMapping("/{*key}")
    fun get(@PathVariable("key") key: String) = s3Service.get(normalizeKey(key))

    @PostMapping(consumes = ["multipart/form-data"])
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    fun upload(@RequestParam file: MultipartFile) = s3Service.upload(file)

    @DeleteMapping("/{*key}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    fun delete(@PathVariable("key") key: String) = s3Service.delete(normalizeKey(key))

    private fun normalizeKey(key: String) = key.trimStart('/')

}
