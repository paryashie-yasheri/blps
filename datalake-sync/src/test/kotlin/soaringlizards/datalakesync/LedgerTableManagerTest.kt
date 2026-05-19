package soaringlizards.datalakesync

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LedgerTableManagerTest {

    @Test
    fun `ledger fields match migration`() {
        val fields = listOf(
            mapOf("name" to "event_id",       "id" to 1,  "type" to "string",    "required" to false),
            mapOf("name" to "operation_id",    "id" to 2,  "type" to "string",    "required" to true),
            mapOf("name" to "event_type",      "id" to 3,  "type" to "string",    "required" to true),
            mapOf("name" to "occurred_at",    "id" to 4,  "type" to "timestamp", "required" to true),
            mapOf("name" to "user_id",         "id" to 5,  "type" to "string",    "required" to true),
            mapOf("name" to "balance_id",      "id" to 6,  "type" to "string",    "required" to true),
            mapOf("name" to "amount",          "id" to 7,  "type" to "int",       "required" to true),
            mapOf("name" to "balance_before",  "id" to 8,  "type" to "int",       "required" to true),
            mapOf("name" to "balance_after",   "id" to 9,  "type" to "int",       "required" to true),
            mapOf("name" to "reward_id",       "id" to 10, "type" to "string",    "required" to false),
            mapOf("name" to "description",     "id" to 11, "type" to "string",    "required" to false),
        )
        assertEquals(11, fields.size)
        assertEquals("event_id", fields[0]["name"])
        assertEquals(1, fields[0]["id"])
        assertEquals("amount", fields[6]["name"])
        assertEquals(7, fields[6]["id"])
        assertEquals("balance_before", fields[7]["name"])
        assertEquals("reward_id", fields[9]["name"])
    }
}
