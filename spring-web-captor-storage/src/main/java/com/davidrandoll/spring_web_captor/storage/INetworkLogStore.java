package com.davidrandoll.spring_web_captor.storage;

import java.util.Optional;

/**
 * Persistence boundary. Consumers supply a Spring bean implementing this
 * interface — backed by JPA, Mongo, a queue, anything — and the storage
 * listener calls into it. The library never assumes how rows are stored.
 */
public interface INetworkLogStore {

    /** Construct a fresh log instance of the consumer's concrete type. */
    INetworkLog newInstance();

    void save(INetworkLog log);

    Optional<INetworkLog> findByRequestId(String requestId);

    void deleteByRequestId(String requestId);
}
