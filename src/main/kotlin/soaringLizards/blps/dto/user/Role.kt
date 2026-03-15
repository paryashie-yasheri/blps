package soaringLizards.blps.dto.user

interface Role {
    fun includes(role: Role): Boolean
}
