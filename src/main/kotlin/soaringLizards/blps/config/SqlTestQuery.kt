package soaringLizards.blps.config

fun normalizeSqlTestQuery(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.length >= 2) {
        val first = trimmed.first()
        val last = trimmed.last()
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return trimmed.substring(1, trimmed.length - 1).trim()
        }
    }
    return trimmed
}
