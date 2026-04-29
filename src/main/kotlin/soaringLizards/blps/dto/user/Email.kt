package soaringLizards.blps.dto.user

@JvmInline
value class Email(val value: String) {
    init {
        require(value.isValidEmail()) { "Invalid email format: $value" }
    }

    companion object {
        fun of(value: String): Result<Email> = runCatching {
            Email(value)
        }
    }
}

private fun String.isValidEmail(): Boolean =
    // More lenient validation that doesn't require TLD
    Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matches(this) || 
    // Original strict validation
    Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(this)
