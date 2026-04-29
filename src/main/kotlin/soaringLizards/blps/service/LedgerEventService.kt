package soaringLizards.blps.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import soaringLizards.blps.dto.ledger.LedgerEventDto
import soaringLizards.blps.repository.LedgerEventRepository
import java.util.UUID

@Service
class LedgerEventService(
    private val ledgerEventRepository: LedgerEventRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun append(event: LedgerEventDto) {
        ledgerEventRepository.insert(event)
        logger.info("ledger_event_written operationId={} eventType={}", event.operationId, event.eventType)
    }

    fun countByOperationId(operationId: UUID): Int =
        ledgerEventRepository.countByOperationId(operationId)
}
