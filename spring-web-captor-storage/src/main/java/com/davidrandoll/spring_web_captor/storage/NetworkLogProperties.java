package com.davidrandoll.spring_web_captor.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "network-log")
public class NetworkLogProperties {

    /** Master switch. When false, nothing is captured at all. */
    private boolean enabled = true;

    /**
     * Legacy / shorthand status filter. See {@link #rules} for the richer
     * format. If both are empty, every status is captured with every field.
     */
    private List<String> captureStatuses = new ArrayList<>();

    /** Path prefixes whose requests/responses are skipped entirely. */
    private List<String> excludePaths = new ArrayList<>();

    /** Header names whose values are replaced with {@code [REDACTED]} (case-insensitive). */
    private List<String> redactHeaders = new ArrayList<>(List.of("Authorization"));

    /** JSON field names redacted at any depth in bodies and additional data. */
    private List<String> redactFields = new ArrayList<>();

    /** Ordered capture rules; first match wins, no match drops the row. */
    private List<CaptureRule> rules = new ArrayList<>();

    @Data
    public static class CaptureRule {
        /** Status matcher entries: {@code "500"}, {@code "400-599"}, {@code ">=400"}, etc. */
        private List<String> match = new ArrayList<>();

        /** Whitelist of field names to keep; null/empty means keep everything. */
        private List<String> fields;
    }
}
