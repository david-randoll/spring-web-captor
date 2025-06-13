package com.davidrandoll.spring_web_captor.event;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class HttpRequestEvent extends BaseHttpEvent {
    // Inherits all relevant fields
}