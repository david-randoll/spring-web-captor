import type { HttpResponseEvent } from '../types/events';
import EventCard from './EventCard';
import EventDetailView from './EventDetailView';
import { Inbox } from 'lucide-react';

interface Props {
  events: HttpResponseEvent[];
  selectedIndex: number | null;
  onSelect: (index: number | null) => void;
}

export default function CapturedEventsPanel({ events, selectedIndex, onSelect }: Props) {
  const reversed = [...events].reverse();
  const selectedEvent = selectedIndex !== null ? reversed[selectedIndex] : null;

  if (events.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-slate-500 gap-4 p-12">
        <Inbox className="w-14 h-14 text-slate-700" />
        <div className="text-center">
          <div className="font-medium text-slate-400 mb-2 text-lg">No events captured yet</div>
          <div className="text-sm text-slate-600 max-w-sm leading-relaxed">
            Run a demo scenario from the left panel. Every HTTP request and response will be captured here with full details.
          </div>
        </div>
      </div>
    );
  }

  if (selectedEvent) {
    return <EventDetailView event={selectedEvent} onBack={() => onSelect(null)} />;
  }

  return (
    <div className="flex flex-col h-full">
      {/* Event list header */}
      <div className="px-5 py-3 border-b border-slate-800 shrink-0">
        <span className="text-sm font-medium text-slate-400">
          {events.length} captured event{events.length === 1 ? '' : 's'} — click to inspect
        </span>
      </div>

      {/* Event list */}
      <div className="flex-1 overflow-auto">
        {reversed.map((event, i) => (
          <EventCard key={`${event.fullUrl}-${i}`} event={event} onClick={() => onSelect(i)} />
        ))}
      </div>
    </div>
  );
}
