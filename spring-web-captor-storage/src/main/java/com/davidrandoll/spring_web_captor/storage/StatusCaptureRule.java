package com.davidrandoll.spring_web_captor.storage;

import java.util.List;

final class StatusCaptureRule {

    private StatusCaptureRule() {
    }

    static boolean shouldCapture(Integer status, List<String> rules) {
        if (rules == null || rules.isEmpty()) return true;
        if (status == null) return true;

        for (String rule : rules) {
            if (matches(status, rule.trim())) return true;
        }
        return false;
    }

    private static boolean matches(int status, String rule) {
        if (rule.startsWith(">=")) {
            return status >= Integer.parseInt(rule.substring(2).trim());
        }
        if (rule.startsWith("<=")) {
            return status <= Integer.parseInt(rule.substring(2).trim());
        }
        if (rule.startsWith(">")) {
            return status > Integer.parseInt(rule.substring(1).trim());
        }
        if (rule.startsWith("<")) {
            return status < Integer.parseInt(rule.substring(1).trim());
        }
        if (rule.contains("-")) {
            String[] parts = rule.split("-", 2);
            int low = Integer.parseInt(parts[0].trim());
            int high = Integer.parseInt(parts[1].trim());
            return status >= low && status <= high;
        }
        return status == Integer.parseInt(rule);
    }
}
