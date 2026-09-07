package com.owlvigil;

import com.owlvigil.management.ManagementModels.WorkspaceCreateRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in: creates and deletes one empty staging workspace. */
@EnabledIfEnvironmentVariable(named = "OWLVIGIL_TYPED_STAGING_TEST", matches = "1")
final class StagingTypedManagementTest {
    @Test void typedReadsAndWorkspaceLifecycle() throws Exception {
        String key = null;
        for (String line : Files.readAllLines(Path.of(System.getenv("OWLVIGIL_TEST_ENV_FILE")))) {
            String text = line.trim();
            if (!text.startsWith("OWLVIGIL_API_KEY=")) continue;
            key = text.substring(text.indexOf('=') + 1).trim();
            if ((key.startsWith("\"") && key.endsWith("\"")) ||
                (key.startsWith("'") && key.endsWith("'"))) key = key.substring(1, key.length()-1);
        }
        assertNotNull(key, "Management credential must be configured");
        try (OwlVigilClient client = OwlVigilClient.builder().apiKey(key)
                .managementBaseUrl("https://stagingapi.owlvigil.com/v1").maxRetries(0).build()) {
            assertEquals(200, client.management().getUserProfile().meta().statusCode());
            assertEquals(200, client.management().getBillingSubscription().meta().statusCode());
            WorkspaceCreateRequest request = new WorkspaceCreateRequest();
            request.name = "typed-sdk-check-" + System.currentTimeMillis();
            request.type = "team";
            var created = client.management().postWorkspaces(request);
            Long id = created.value().id != null ? created.value().id : created.value().workspaceId;
            assertNotNull(id);
            try {
                assertEquals(201, created.meta().statusCode());
                assertEquals(id, client.management().getWorkspacesWorkspaceId(id).value().id);
            } finally {
                var deleted = client.management().deleteWorkspacesWorkspaceId(id);
                assertEquals(204, deleted.meta().statusCode());
                assertNull(deleted.value());
            }
            System.out.println("Typed staging: profile, subscription, workspace create/get/delete passed; empty workspace cleaned up.");
        }
    }
}
