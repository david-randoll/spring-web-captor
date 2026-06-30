package com.davidrandoll.spring_web_captor.storage.support;

import com.davidrandoll.spring_web_captor.storage.INetworkLog;
import com.davidrandoll.spring_web_captor.storage.INetworkLogStore;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hand-written native-safe fake of {@link INetworkLogStore} (no Mockito). Keeps saved logs in a concurrent map
 * keyed by requestId so both the synchronous unit tests and the async integration/native boot test can read
 * them back.
 */
public class InMemoryNetworkLogStore implements INetworkLogStore {

    private final Map<String, INetworkLog> byRequestId = new ConcurrentHashMap<>();

    @Override
    public INetworkLog newInstance() {
        return new TestNetworkLog();
    }

    @Override
    public void save(INetworkLog log) {
        if (log.getRequestId() != null) {
            byRequestId.put(log.getRequestId(), log);
        }
    }

    @Override
    public Optional<INetworkLog> findByRequestId(String requestId) {
        return Optional.ofNullable(byRequestId.get(requestId));
    }

    @Override
    public void deleteByRequestId(String requestId) {
        byRequestId.remove(requestId);
    }

    public int size() {
        return byRequestId.size();
    }

    public void clear() {
        byRequestId.clear();
    }

    public Optional<INetworkLog> findAny() {
        return byRequestId.values().stream().findFirst();
    }
}
