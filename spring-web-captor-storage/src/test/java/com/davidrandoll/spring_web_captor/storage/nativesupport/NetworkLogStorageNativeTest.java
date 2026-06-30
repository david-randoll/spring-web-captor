package com.davidrandoll.spring_web_captor.storage.nativesupport;

import com.davidrandoll.spring_web_captor.storage.INetworkLog;
import com.davidrandoll.spring_web_captor.storage.NetworkLogEventListener;
import com.davidrandoll.spring_web_captor.storage.support.InMemoryNetworkLogStore;
import com.davidrandoll.spring_web_captor.storage.testapp.NetworkLogStorageTestApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the storage auto-configuration ENABLED over a real captor + MockMvc and drives a real HTTP request so
 * the whole capture -> event -> async storage-listener -> store path executes. This is the native-capable boot
 * test (no Mockito, real objects only): it exercises the autoconfig bean wiring, the requestId extension, the
 * NetworkLogProperties binding, and the listener's request/response persistence — exactly the paths that would
 * break under a closed-world native image if a reflection/binding hint were missing.
 */
@SpringBootTest(classes = NetworkLogStorageTestApp.class, properties = {
        "spring.application.name=network-log-storage-native-test",
        "network-log.enabled=true",
        "network-log.redact-headers=Authorization"
})
@AutoConfigureMockMvc
@Tag("native")
class NetworkLogStorageNativeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryNetworkLogStore store;

    @Autowired
    private NetworkLogEventListener listener; // proves the @ConditionalOnBean listener wired

    @BeforeEach
    void reset() {
        store.clear();
    }

    @Test
    void contextWiresStorageBeans() {
        assertThat(listener).isNotNull();
        assertThat(store).isNotNull();
    }

    @Test
    void realRequestIsCapturedEnrichedAndPersisted() throws Exception {
        mockMvc.perform(get("/api/widgets/42?color=blue"))
                .andExpect(status().isOk());

        INetworkLog saved = awaitOneRow();

        assertThat(saved.getMethod()).isEqualTo("GET");
        assertThat(saved.getPath()).isEqualTo("/api/widgets/42");
        assertThat(saved.getResponseStatus()).isEqualTo(200);
        assertThat(saved.getRequestId()).isNotBlank();
        // enricher ran
        assertThat(saved.getAdditionalData()).containsEntry("enrichedTenant", "acme");
    }

    private INetworkLog awaitOneRow() {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            Optional<INetworkLog> row = store.findAny();
            // wait until the response phase has stamped the status, not just the request phase
            if (row.isPresent() && row.get().getResponseStatus() != null) {
                return row.get();
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return store.findAny().orElseThrow(() -> new AssertionError("no network-log row was persisted"));
    }
}
