package com.davidrandoll.spring_web_captor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Spring Security is on the test classpath (used by RuntimeExceptionResolverSecurity* tests
 * to construct real AccessDeniedException / AuthenticationException instances) but its
 * servlet auto-configuration is disabled by default here so that pre-existing tests —
 * which never had a security filter chain — behave exactly as before. Tests that actually
 * want the security filter chain (e.g. {@code RuntimeExceptionResolverSecurityMvcTest})
 * re-enable it via {@code @ImportAutoConfiguration}.
 */
@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
public class WebCaptorApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebCaptorApplication.class, args);
    }
}
