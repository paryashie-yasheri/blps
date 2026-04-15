package soaringLizards.blps

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import soaringLizards.blps.integration.IntegrationPostgres
import soaringLizards.blps.integration.TestcontainersCleanupExtension

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(TestcontainersCleanupExtension::class)
class RolePermissionsTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            IntegrationPostgres.registerProperties(registry)
        }
    }

    @Test
    fun `public endpoint should be accessible without authentication`() {
        mockMvc.get("/api/test/roles/public")
            .andExpect {
                status { isOk() }
                content { string("Public endpoint - no authentication required") }
            }
    }

    @Test
    fun `user endpoint should require authentication`() {
        mockMvc.get("/api/test/roles/user")
            .andExpect {
                status { is3xxRedirection() }
            }
    }

    @Test
    @WithMockUser(roles = ["OPENEDX_USER"])
    fun `user endpoint should be accessible with OPENEDX_USER role - authenticated`() {
        mockMvc.get("/api/test/roles/user")
            .andExpect {
                status { isOk() }
                content { string("User endpoint - requires OPENEDX_USER role") }
            }
    }

    @Test
    @WithMockUser(roles = ["STAFF"])
    fun `user endpoint should be forbidden for STAFF role`() {
        mockMvc.get("/api/test/roles/user")
            .andExpect {
                status { isForbidden() }
            }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `user endpoint should be forbidden for ADMIN role`() {
        mockMvc.get("/api/test/roles/user")
            .andExpect {
                status { isForbidden() }
            }
    }

    @Test
    @WithMockUser(roles = ["STAFF"])
    fun `staff endpoint should be accessible with STAFF role`() {
        mockMvc.get("/api/test/roles/staff")
            .andExpect {
                status { isOk() }
                content { string("Staff endpoint - requires STAFF role") }
            }
    }

    @Test
    @WithMockUser(roles = ["OPENEDX_USER"])
    fun `staff endpoint should be forbidden for OPENEDX_USER role`() {
        mockMvc.get("/api/test/roles/staff")
            .andExpect {
                status { isForbidden() }
            }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `staff endpoint should be forbidden for ADMIN role`() {
        mockMvc.get("/api/test/roles/staff")
            .andExpect {
                status { isForbidden() }
            }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `admin endpoint should be accessible with ADMIN role`() {
        mockMvc.get("/api/test/roles/admin")
            .andExpect {
                status { isOk() }
                content { string("Admin endpoint - requires ADMIN role") }
            }
    }

    @Test
    @WithMockUser(roles = ["OPENEDX_USER"])
    fun `admin endpoint should be forbidden for OPENEDX_USER role`() {
        mockMvc.get("/api/test/roles/admin")
            .andExpect {
                status { isForbidden() }
            }
    }

    @Test
    @WithMockUser(roles = ["STAFF"])
    fun `admin endpoint should be forbidden for STAFF role`() {
        mockMvc.get("/api/test/roles/admin")
            .andExpect {
                status { isForbidden() }
            }
    }

    @Test
    @WithMockUser(roles = ["OPENEDX_USER"])
    fun `user-or-staff endpoint should be accessible with OPENEDX_USER role`() {
        mockMvc.get("/api/test/roles/user-or-staff")
            .andExpect {
                status { isOk() }
                content { string("User or Staff endpoint - requires OPENEDX_USER or STAFF role") }
            }
    }

    @Test
    @WithMockUser(roles = ["STAFF"])
    fun `user-or-staff endpoint should be accessible with STAFF role`() {
        mockMvc.get("/api/test/roles/user-or-staff")
            .andExpect {
                status { isOk() }
                content { string("User or Staff endpoint - requires OPENEDX_USER or STAFF role") }
            }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `user-or-staff endpoint should be forbidden for ADMIN role`() {
        mockMvc.get("/api/test/roles/user-or-staff")
            .andExpect {
                status { isForbidden() }
            }
    }

    @Test
    @WithMockUser(roles = ["STAFF"])
    fun `staff-or-admin endpoint should be accessible with STAFF role`() {
        mockMvc.get("/api/test/roles/staff-or-admin")
            .andExpect {
                status { isOk() }
                content { string("Staff or Admin endpoint - requires STAFF or ADMIN role") }
            }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `staff-or-admin endpoint should be accessible with ADMIN role`() {
        mockMvc.get("/api/test/roles/staff-or-admin")
            .andExpect {
                status { isOk() }
                content { string("Staff or Admin endpoint - requires STAFF or ADMIN role") }
            }
    }

    @Test
    @WithMockUser(roles = ["OPENEDX_USER"])
    fun `staff-or-admin endpoint should be forbidden for OPENEDX_USER role`() {
        mockMvc.get("/api/test/roles/staff-or-admin")
            .andExpect {
                status { isForbidden() }
            }
    }

    @Test
    @WithMockUser(roles = ["OPENEDX_USER"])
    fun `any-authenticated endpoint should be accessible with OPENEDX_USER role`() {
        mockMvc.get("/api/test/roles/any-authenticated")
            .andExpect {
                status { isOk() }
                content { string("Any authenticated user can access this") }
            }
    }

    @Test
    @WithMockUser(roles = ["STAFF"])
    fun `any-authenticated endpoint should be accessible with STAFF role`() {
        mockMvc.get("/api/test/roles/any-authenticated")
            .andExpect {
                status { isOk() }
                content { string("Any authenticated user can access this") }
            }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `any-authenticated endpoint should be accessible with ADMIN role`() {
        mockMvc.get("/api/test/roles/any-authenticated")
            .andExpect {
                status { isOk() }
                content { string("Any authenticated user can access this") }
            }
    }

    @Test
    fun `any-authenticated endpoint should require authentication`() {
        mockMvc.get("/api/test/roles/any-authenticated")
            .andExpect {
                status { is3xxRedirection() }
            }
    }

    @Test
    @WithMockUser(roles = ["OPENEDX_USER", "STAFF", "ADMIN"])
    fun `user with multiple roles should access all respective endpoints`() {
        mockMvc.get("/api/test/roles/user")
            .andExpect { status { isOk() } }

        mockMvc.get("/api/test/roles/staff")
            .andExpect { status { isOk() } }

        mockMvc.get("/api/test/roles/admin")
            .andExpect { status { isOk() } }
    }
}
