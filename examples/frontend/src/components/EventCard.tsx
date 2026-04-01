import type { HttpResponseEvent } from '../types/events';
import MethodBadge from './MethodBadge';
import StatusBadge from './StatusBadge';
import { Clock } from 'lucide-react';

function formatDuration(d: unknown): string | null {
  if (!d) return null;
  const s = String(d);
  const match = s.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:([\d.]+)S)?/);
  if (match) {
    const h = parseInt(match[1] || '0');
    const m = parseInt(match[2] || '0');
    const sec = parseFloat(match[3] || '0');
    const totalMs = (h * 3600 + m * 60 + sec) * 1000;
    if (totalMs < 1000) return `${Math.round(totalMs)}ms`;
    return `${(totalMs / 1000).toFixed(2)}s`;
  }
  return null;
}

export default function EventCard({
  event,
  index,
  onClick,
}: {
  event: HttpResponseEvent;
  index: number;
  onClick: () => void;
}) {
  const duration = formatDuration(event.duration);

  return (
    <button
      onClick={onClick}
      className="w-full text-left px-4 py-3 hover:bg-slate-800/70 transition-colors border-b border-slate-800/50"
    >
      <div className="flex items-center gap-2 mb-1">
        <MethodBadge method={event.method} />
        <span className="font-mono text-sm text-slate-300 truncate flex-1">{event.path}</span>
        <StatusBadge status={event.responseStatus} />
      </div>
      <div className="flex items-center gap-3 text-xs text-slate-500">
        <span className="font-mono truncate">{event.fullUrl}</span>
        {duration && (
          <span className="flex items-center gap-1 shrink-0">
            <Clock className="w-3 h-3" /> {duration}
          </span>
        )}
      </div>
    </button>
  );
}
