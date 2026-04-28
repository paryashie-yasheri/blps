package soaringLizards.blps.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test/roles")
class RoleTestController {

    @GetMapping("/public")
    fun publicEndpoint(): ResponseEntity<String> {
        return ResponseEntity.ok("Public endpoint - no authentication required")
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('OPENEDX_USER')")
    fun userEndpoint(): ResponseEntity<String> {
        return ResponseEntity.ok("User endpoint - requires OPENEDX_USER role")
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('STAFF')")
    fun staffEndpoint(): ResponseEntity<String> {
        return ResponseEntity.ok("Staff endpoint - requires STAFF role")
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    fun adminEndpoint(): ResponseEntity<String> {
        return ResponseEntity.ok("Admin endpoint - requires ADMIN role")
    }

    @GetMapping("/user-or-staff")
    @PreAuthorize("hasAnyRole('OPENEDX_USER', 'STAFF')")
    fun userOrStaffEndpoint(): ResponseEntity<String> {
        return ResponseEntity.ok("User or Staff endpoint - requires OPENEDX_USER or STAFF role")
    }

    @GetMapping("/staff-or-admin")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun staffOrAdminEndpoint(): ResponseEntity<String> {
        return ResponseEntity.ok("Staff or Admin endpoint - requires STAFF or ADMIN role")
    }

    @GetMapping("/any-authenticated")
    fun anyAuthenticatedEndpoint(): ResponseEntity<String> {
        return ResponseEntity.ok("Any authenticated user can access this")
    }
}
