package com.acme.security;

/**
 * Test fixture only — pretends to be a security exception from a custom framework outside the
 * Spring Security namespace, used to verify {@code web-captor.defer-outer-filter-packages} can
 * be extended to cover arbitrary packages without library code changes.
 */
public class CustomDenied extends RuntimeException {
    public CustomDenied(String msg) {
        super(msg);
    }
}
