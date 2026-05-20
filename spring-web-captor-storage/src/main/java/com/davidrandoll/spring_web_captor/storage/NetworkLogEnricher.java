package com.davidrandoll.spring_web_captor.storage;

import com.davidrandoll.spring_web_captor.event.BaseHttpEvent;

/**
 * Extension point for populating context-specific data on a network log
 * before it is saved. Every Spring bean implementing this interface is
 * invoked, in order, on both request-phase and response-phase entries.
 *
 * <p>Typical implementations: populate a tenant column on a consumer-defined
 * subtype, copy authenticated-user details into
 * {@link INetworkLog#getAdditionalData()}, attach distributed-trace ids.</p>
 */
@FunctionalInterface
public interface NetworkLogEnricher {

    void enrich(INetworkLog log, BaseHttpEvent event);
}
