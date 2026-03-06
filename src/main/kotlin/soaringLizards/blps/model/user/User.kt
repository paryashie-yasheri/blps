package soaringLizards.blps.model.user

data class User(
    val id: String,
    val role: String,
    val passwordHash: String,
    val email: String,
)