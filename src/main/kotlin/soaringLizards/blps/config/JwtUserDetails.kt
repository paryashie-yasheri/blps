package soaringLizards.blps.config

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class JwtUserDetails(
    private val _username: String,
    val role: String,
    val token: String,
    private val authorities: Collection<GrantedAuthority> = listOf(SimpleGrantedAuthority("ROLE_$role")),
) : UserDetails {

    override fun getUsername(): String = _username

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities

    override fun getPassword(): String? = null

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}
