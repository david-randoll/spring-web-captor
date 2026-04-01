import { useState, useEffect, useCallback } from 'react';
import type { CapturedEventsResponse, HttpResponseEvent } from '../types/events';
import { fetchCapturedEvents, clearCapturedEvents } from '../api/client';

export function useCapturedEvents() {
  const [events, setEvents] = useState<HttpResponseEvent[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const poll = useCallback(async () => {
    try {
      const data: CapturedEventsResponse = await fetchCapturedEvents();
      // Response events contain the full picture (request + response data)
      setEvents(data.responseEvents || []);
    } catch {
      // Backend might not be running
    }
  }, []);

  useEffect(() => {
    poll();
    const interval = setInterval(poll, 1000);
    return () => clearInterval(interval);
  }, [poll]);

  const clear = useCallback(async () => {
    setIsLoading(true);
    await clearCapturedEvents();
    setEvents([]);
    setIsLoading(false);
  }, []);

  return { events, isLoading, clear, refresh: poll };
}
