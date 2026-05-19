package soaringlizards.datalakesync.spark

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class PreparedQueryTest {

    @Test
    fun `single int parameter`() {
        val sql = query("SELECT * FROM t WHERE id = ?", QueryParam.int(42)).build()
        assertEquals("SELECT * FROM t WHERE id = 42", sql)
    }

    @Test
    fun `multiple parameters in order`() {
        val sql = query(
            "INSERT INTO t VALUES (?, ?, ?)",
            QueryParam.int(1),
            QueryParam.str("hello"),
            QueryParam.`null`(),
        ).build()
        assertEquals("INSERT INTO t VALUES (1, 'hello', NULL)", sql)
    }

    @Test
    fun `string escapes single quotes`() {
        val sql = query("SELECT * FROM t WHERE name = ?", QueryParam.str("O'Reilly")).build()
        assertEquals("SELECT * FROM t WHERE name = 'O''Reilly'", sql)
    }

    @Test
    fun `raw param passes through unchanged`() {
        val sql = query("SELECT ?", QueryParam.raw("current_timestamp()")).build()
        assertEquals("SELECT current_timestamp()", sql)
    }

    @Test
    fun `bool param`() {
        val sql = query("SELECT * FROM t WHERE active = ?", QueryParam.bool(true)).build()
        assertEquals("SELECT * FROM t WHERE active = true", sql)
    }

    @Test
    fun `double param`() {
        val sql = query("SELECT * FROM t WHERE price = ?", QueryParam.double(3.14)).build()
        assertEquals("SELECT * FROM t WHERE price = 3.14", sql)
    }

    @Test
    fun `throws when not enough params`() {
        assertThrows<IllegalStateException> {
            query("SELECT * FROM t WHERE a = ? AND b = ?", QueryParam.int(1)).build()
        }
    }

    @Test
    fun `throws when too many params`() {
        assertThrows<IllegalStateException> {
            query("SELECT * FROM t WHERE a = ?", QueryParam.int(1), QueryParam.int(2)).build()
        }
    }

    @Test
    fun `fluent bind`() {
        val sql = query("INSERT INTO t VALUES (?, ?, ?)")
            .bind(QueryParam.int(99))
            .bind(QueryParam.str("test"))
            .bind(QueryParam.`null`())
            .build()
        assertEquals("INSERT INTO t VALUES (99, 'test', NULL)", sql)
    }

    @Test
    fun `convenience bind methods`() {
        val sql = query("INSERT INTO t VALUES (?, ?, ?, ?)")
            .bind(1)
            .bind("hello")
            .bind(true)
            .bind(3.5)
            .build()
        assertEquals("INSERT INTO t VALUES (1, 'hello', true, 3.5)", sql)
    }

    @Test
    fun `convenience factory methods`() {
        val sql = query(
            "SELECT * FROM t WHERE id = ? AND name = ? AND active = ?",
            QueryParam.int(5),
            QueryParam.str("Alice"),
            QueryParam.bool(false),
        ).build()
        assertEquals("SELECT * FROM t WHERE id = 5 AND name = 'Alice' AND active = false", sql)
    }
}
