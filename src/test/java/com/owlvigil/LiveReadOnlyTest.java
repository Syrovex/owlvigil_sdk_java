package com.owlvigil;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "OWLVIGIL_LIVE_TEST", matches = "1")
final class LiveReadOnlyTest {
    @Test
    void gatewayAndManagementReadOnlyFlow() {
        try (OwlVigilClient client = OwlVigilClient.builder().build()) {
            assertThat(client.gateway().listModels().value().data()).isNotEmpty();
            var workspaces = client.management().listWorkspaces().value().items();
            assertThat(workspaces).isNotEmpty();
            assertThat(client.management().getUsageSummary(workspaces.get(0).id()).value().requests()).isGreaterThanOrEqualTo(0);
        }
    }
}
