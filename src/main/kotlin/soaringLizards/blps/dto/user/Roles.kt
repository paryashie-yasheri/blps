package soaringLizards.blps.dto.user

enum class Roles(
    private val children: Set<Roles> = emptySet()
) : Role {

    override fun includes(role: Role): Boolean {
        if (this == role) return true
        return children.any { it.includes(role) }
    }
}