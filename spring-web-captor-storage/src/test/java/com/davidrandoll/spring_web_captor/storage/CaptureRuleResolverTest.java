package com.davidrandoll.spring_web_captor.storage;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CaptureRuleResolverTest {

    @Test
    void skipWhenPropertiesNull() {
        var decision = CaptureRuleResolver.resolve(200, null);
        assertThat(decision.capture()).isFalse();
    }

    @Test
    void skipWhenDisabled() {
        NetworkLogProperties props = new NetworkLogProperties();
        props.setEnabled(false);
        assertThat(CaptureRuleResolver.resolve(200, props).capture()).isFalse();
    }

    @Test
    void captureAllWhenNoRulesAndNoStatusFilter() {
        NetworkLogProperties props = new NetworkLogProperties();
        var decision = CaptureRuleResolver.resolve(200, props);
        assertThat(decision.capture()).isTrue();
        assertThat(decision.fieldWhitelist()).isNull();
    }

    @Test
    void legacyCaptureStatusesFilter() {
        NetworkLogProperties props = new NetworkLogProperties();
        props.setCaptureStatuses(List.of(">=400"));
        assertThat(CaptureRuleResolver.resolve(500, props).capture()).isTrue();
        assertThat(CaptureRuleResolver.resolve(200, props).capture()).isFalse();
    }

    @Test
    void rulesMatchWithFullFieldCapture() {
        NetworkLogProperties props = new NetworkLogProperties();
        NetworkLogProperties.CaptureRule rule = new NetworkLogProperties.CaptureRule();
        rule.setMatch(List.of(">=500"));
        props.setRules(List.of(rule));

        var decision = CaptureRuleResolver.resolve(503, props);
        assertThat(decision.capture()).isTrue();
        assertThat(decision.fieldWhitelist()).isNull(); // null/empty fields => capture all
    }

    @Test
    void rulesMatchWithFieldWhitelist() {
        NetworkLogProperties props = new NetworkLogProperties();
        NetworkLogProperties.CaptureRule rule = new NetworkLogProperties.CaptureRule();
        rule.setMatch(List.of("400-599"));
        rule.setFields(List.of("path", "responseStatus"));
        props.setRules(List.of(rule));

        var decision = CaptureRuleResolver.resolve(404, props);
        assertThat(decision.capture()).isTrue();
        assertThat(decision.fieldWhitelist()).containsExactlyInAnyOrder("path", "responseStatus");
    }

    @Test
    void noMatchingRuleSkips() {
        NetworkLogProperties props = new NetworkLogProperties();
        NetworkLogProperties.CaptureRule rule = new NetworkLogProperties.CaptureRule();
        rule.setMatch(List.of(">=500"));
        props.setRules(List.of(rule));

        assertThat(CaptureRuleResolver.resolve(200, props).capture()).isFalse();
    }

    @Test
    void firstMatchingRuleWins() {
        NetworkLogProperties props = new NetworkLogProperties();
        NetworkLogProperties.CaptureRule first = new NetworkLogProperties.CaptureRule();
        first.setMatch(List.of(">=500"));
        first.setFields(List.of("path"));
        NetworkLogProperties.CaptureRule second = new NetworkLogProperties.CaptureRule();
        second.setMatch(List.of(">=400"));
        second.setFields(List.of("responseBody"));
        props.setRules(List.of(first, second));

        var decision = CaptureRuleResolver.resolve(503, props);
        assertThat(decision.fieldWhitelist()).containsExactly("path");
    }
}
