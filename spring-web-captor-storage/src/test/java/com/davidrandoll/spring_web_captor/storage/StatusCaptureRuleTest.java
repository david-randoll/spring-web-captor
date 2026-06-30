package com.davidrandoll.spring_web_captor.storage;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatusCaptureRuleTest {

    @Test
    void nullOrEmptyRulesCaptureEverything() {
        assertThat(StatusCaptureRule.shouldCapture(200, null)).isTrue();
        assertThat(StatusCaptureRule.shouldCapture(200, List.of())).isTrue();
    }

    @Test
    void nullStatusIsCaptured() {
        assertThat(StatusCaptureRule.shouldCapture(null, List.of("500"))).isTrue();
    }

    @Test
    void exactMatch() {
        assertThat(StatusCaptureRule.shouldCapture(404, List.of("404"))).isTrue();
        assertThat(StatusCaptureRule.shouldCapture(200, List.of("404"))).isFalse();
    }

    @Test
    void greaterThanOrEqual() {
        assertThat(StatusCaptureRule.shouldCapture(500, List.of(">=400"))).isTrue();
        assertThat(StatusCaptureRule.shouldCapture(399, List.of(">=400"))).isFalse();
    }

    @Test
    void lessThanOrEqual() {
        assertThat(StatusCaptureRule.shouldCapture(200, List.of("<=299"))).isTrue();
        assertThat(StatusCaptureRule.shouldCapture(300, List.of("<=299"))).isFalse();
    }

    @Test
    void strictlyGreaterThan() {
        assertThat(StatusCaptureRule.shouldCapture(500, List.of(">499"))).isTrue();
        assertThat(StatusCaptureRule.shouldCapture(499, List.of(">499"))).isFalse();
    }

    @Test
    void strictlyLessThan() {
        assertThat(StatusCaptureRule.shouldCapture(100, List.of("<200"))).isTrue();
        assertThat(StatusCaptureRule.shouldCapture(200, List.of("<200"))).isFalse();
    }

    @Test
    void inclusiveRange() {
        assertThat(StatusCaptureRule.shouldCapture(450, List.of("400-599"))).isTrue();
        assertThat(StatusCaptureRule.shouldCapture(400, List.of("400-599"))).isTrue();
        assertThat(StatusCaptureRule.shouldCapture(599, List.of("400-599"))).isTrue();
        assertThat(StatusCaptureRule.shouldCapture(399, List.of("400-599"))).isFalse();
        assertThat(StatusCaptureRule.shouldCapture(600, List.of("400-599"))).isFalse();
    }

    @Test
    void firstMatchingRuleWins() {
        assertThat(StatusCaptureRule.shouldCapture(503, List.of("200", ">=500"))).isTrue();
    }

    @Test
    void whitespaceInRuleIsTrimmed() {
        assertThat(StatusCaptureRule.shouldCapture(500, List.of("  >= 400 "))).isTrue();
        assertThat(StatusCaptureRule.shouldCapture(450, List.of(" 400 - 599 "))).isTrue();
    }
}
