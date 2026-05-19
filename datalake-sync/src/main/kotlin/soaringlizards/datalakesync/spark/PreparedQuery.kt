package soaringlizards.datalakesync.spark

import org.apache.commons.text.translate.AggregateTranslator
import org.apache.commons.text.translate.CharSequenceTranslator
import org.apache.commons.text.translate.LookupTranslator

sealed class QueryParam {
    abstract fun toSqlString(): String

    data class IntParam(val value: Int) : QueryParam() {
        override fun toSqlString() = value.toString()
    }

    data class LongParam(val value: Long) : QueryParam() {
        override fun toSqlString() = value.toString()
    }

    data class DoubleParam(val value: Double) : QueryParam() {
        override fun toSqlString() = value.toString()
    }

    data class BoolParam(val value: Boolean) : QueryParam() {
        override fun toSqlString() = value.toString()
    }

    data class StringParam(val value: String) : QueryParam() {
        private companion object {
            private val SQL_ESCAPE: CharSequenceTranslator = AggregateTranslator(
                LookupTranslator(
                    mapOf(
                        "\\" to "\\\\",
                        "'" to "''",
                        "\r" to "\\r",
                        "\n" to "\\n",
                        "\t" to "\\t",
                        "\u0000" to "\\0",
                        "\u001a" to "\\Z",
                    ),
                ),
            )
        }

        override fun toSqlString(): String {
            val escaped = SQL_ESCAPE.translate(value)
            return "'$escaped'"
        }
    }

    data class RawParam(val value: String) : QueryParam() {
        override fun toSqlString() = value
    }

    data object NullParam : QueryParam() {
        override fun toSqlString() = "NULL"
    }

    companion object {
        fun int(v: Int) = IntParam(v)
        fun long(v: Long) = LongParam(v)
        fun double(v: Double) = DoubleParam(v)
        fun bool(v: Boolean) = BoolParam(v)
        fun str(v: String) = StringParam(v)
        fun raw(v: String) = RawParam(v)
        fun null_() = NullParam
    }
}

class PreparedQuery(private val template: String) {

    private val params = mutableListOf<QueryParam>()

    fun bind(vararg values: QueryParam): PreparedQuery {
        params.addAll(values)
        return this
    }

    fun bind(value: QueryParam): PreparedQuery {
        params.add(value)
        return this
    }

    fun bind(value: Int) = bind(QueryParam.int(value))
    fun bind(value: Long) = bind(QueryParam.long(value))
    fun bind(value: Double) = bind(QueryParam.double(value))
    fun bind(value: Boolean) = bind(QueryParam.bool(value))
    fun bind(value: String) = bind(QueryParam.str(value))

    fun build(): String {
        val sb = StringBuilder()
        var paramIndex = 0

        for (i in template.indices) {
            if (template[i] == '?') {
                sb.append(
                    params.getOrElse(paramIndex) {
                        throw IllegalStateException("Not enough parameters: expected at least ${paramIndex + 1}, got ${params.size}")
                    }.toSqlString(),
                )
                paramIndex++
            } else {
                sb.append(template[i])
            }
        }

        if (paramIndex < params.size) {
            throw IllegalStateException("Too many parameters: $paramIndex used, ${params.size} provided")
        }

        return sb.toString()
    }

    fun executeWith(executor: SparkSqlExecutor.Builder): SparkSqlResult {
        return executor.execute(build())
    }
}

fun query(template: String, vararg params: QueryParam): PreparedQuery {
    return PreparedQuery(template).bind(*params)
}
