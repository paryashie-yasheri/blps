package soaringLizards.blps.integration

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class TestcontainersCleanupExtension : BeforeAllCallback {

    override fun beforeAll(context: ExtensionContext) {
        val store = context
            .root
            .getStore(ExtensionContext.Namespace.create(TestcontainersCleanupExtension::class.java))

        if (store.get(CLEANUP_KEY) == null) {
            store.put(CLEANUP_KEY, ExtensionContext.Store.CloseableResource {
                runCatching { IntegrationPostgres.stop() }
                runCatching { DegradedRewardsPostgres.stop() }
                runCatching { IntegrationKeycloak.stop() }
            })
        }
    }

    private companion object {
        private const val CLEANUP_KEY = "testcontainers_cleanup"
    }
}
