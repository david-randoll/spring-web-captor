package com.davidrandoll.spring_web_captor.storage;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class RequestIdProviderTest {

    @Test
    void uuidProviderReturnsDistinctParsableUuids() {
        RequestIdProvider provider = RequestIdProvider.uuid();
        MockHttpServletRequest req = new MockHttpServletRequest();

        String a = provider.nextRequestId(req);
        String b = provider.nextRequestId(req);

        assertThat(a).isNotEqualTo(b);
        assertThatNoException().isThrownBy(() -> UUID.fromString(a));
    }
}
