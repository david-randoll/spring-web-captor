import { useState } from 'react';
import type { HttpResponseEvent } from '../types/events';
import EventCard from './EventCard';
import EventDetailView from './EventDetailView';
import { Radio, Trash2, Inbox } from 'lucide-react';

interface Props {
  events: HttpResponseEvent[];
  onClear: () => void;
}

export default function CapturedEventsPanel({ events, onClear }: Props) {
  const [selectedIndex, setSelectedIndex] = useState<number | null>(null);

  const reversed = [...events].reverse();
  const selectedEvent = selectedIndex !== null ? reversed[selectedIndex] : null;

  if (selectedEvent) {
    return (
      <div className="flex flex-col h-full">
        <EventDetailView event={selectedEvent} onBack={() => setSelectedIndex(null)} />
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="px-4 py-3 border-b border-slate-800 flex items-center justify-between shrink-0">
        <div className="flex items-center gap-2">
          <Radio className="w-4 h-4 text-blue-400" />
          <span className="font-semibold text-sm text-slate-200">Captured Events</span>
          {events.length > 0 && (
            <span className="bg-blue-500/15 text-blue-400 border border-blue-500/30 rounded-full px-2 py-0 text-xs font-bold">
              {events.length}
            </span>
          )}
        </div>
        {events.length > 0 && (
          <button
            onClick={onClear}
            className="flex items-center gap-1 text-xs text-slate-500 hover:text-red-400 transition-colors"
          >
            <Trash2 className="w-3 h-3" /> Clear
          </button>
        )}
      </div>

      {/* Events list */}
      <div className="flex-1 overflow-auto">
        {events.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-slate-500 gap-3 p-8">
            <Inbox className="w-10 h-10 text-slate-600" />
            <div className="text-sm text-center">
              <div className="font-medium text-slate-400 mb-1">No events captured yet</div>
              <div className="text-xs">Send a request or run a demo to see captured events here</div>
            </div>
          </div>
        ) : (
          reversed.map((event, i) => (
            <EventCard key={i} event={event} index={i} onClick={() => setSelectedIndex(i)} />
          ))
        )}
      </div>
    </div>
  );
}
