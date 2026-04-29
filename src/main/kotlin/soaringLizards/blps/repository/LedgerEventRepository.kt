package soaringLizards.blps.repository

import org.jooq.DSLContext
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.table
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository
import soaringLizards.blps.dto.ledger.LedgerEventDto
import java.util.UUID

@Repository
class LedgerEventRepository(
    @Qualifier("journalDslContext")
    private val journalDsl: DSLContext,
) {
    private val ledgerEvents = table(name("ledger_events"))

    fun insert(event: LedgerEventDto) =
        journalDsl.insertInto(ledgerEvents)
            .set(field(name("operation_id"), UUID::class.java), event.operationId)
            .set(field(name("event_type"), String::class.java), event.eventType)
            .set(field(name("occurred_at"), java.time.OffsetDateTime::class.java), event.occurredAt)
            .set(field(name("user_id"), UUID::class.java), event.userId)
            .set(field(name("balance_id"), UUID::class.java), event.balanceId)
            .set(field(name("amount"), Int::class.java), event.amount)
            .set(field(name("balance_before"), Int::class.java), event.balanceBefore)
            .set(field(name("balance_after"), Int::class.java), event.balanceAfter)
            .set(field(name("reward_id"), UUID::class.java), event.rewardId)
            .set(field(name("description"), String::class.java), event.description)
            .execute()

    fun countByOperationId(operationId: UUID): Int =
        journalDsl.fetchCount(
            journalDsl.selectFrom(ledgerEvents)
                .where(field(name("operation_id"), UUID::class.java).eq(operationId)),
        )
}
