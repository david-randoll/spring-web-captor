package com.davidrandoll.spring_web_captor.storage;

import java.util.Set;

final class CaptureRuleResolver {

    private CaptureRuleResolver() {
    }

    record Decision(boolean capture, Set<String> fieldWhitelist) {
        static final Decision SKIP = new Decision(false, null);
        static final Decision CAPTURE_ALL = new Decision(true, null);
    }

    static Decision resolve(Integer status, NetworkLogProperties properties) {
        if (properties == null || !properties.isEnabled()) {
            return Decision.SKIP;
        }

        var rules = properties.getRules();
        if (rules != null && !rules.isEmpty()) {
            for (var rule : rules) {
                if (StatusCaptureRule.shouldCapture(status, rule.getMatch())) {
                    var fields = rule.getFields();
                    if (fields == null || fields.isEmpty()) {
                        return Decision.CAPTURE_ALL;
                    }
                    return new Decision(true, Set.copyOf(fields));
                }
            }
            return Decision.SKIP;
        }

        return StatusCaptureRule.shouldCapture(status, properties.getCaptureStatuses())
                ? Decision.CAPTURE_ALL
                : Decision.SKIP;
    }
}
