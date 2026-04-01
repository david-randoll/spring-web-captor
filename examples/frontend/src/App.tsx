import { useState } from 'react';
import { useCapturedEvents } from './hooks/useCapturedEvents';
import CapturedEventsPanel from './components/CapturedEventsPanel';
import DemoScenarios from './components/DemoScenarios';
import { Radio, Trash2 } from 'lucide-react';

export default function App() {
  const { events, clear } = useCapturedEvents();
  const [selectedIndex, setSelectedIndex] = useState<number | null>(null);

  const handleClear = () => {
    clear();
    setSelectedIndex(null);
  };

  return (
    <div className="h-screen flex flex-col bg-[#0a0a0f] text-slate-200">
      {/* Header */}
      <header className="h-14 border-b border-slate-800 flex items-center justify-between px-6 shrink-0">
        <div className="flex items-center gap-3">
          <Radio className="w-5 h-5 text-blue-400" />
          <h1 className="text-base font-bold tracking-tight">Spring Web Captor</h1>
          <span className="text-[11px] bg-blue-500/15 text-blue-400 border border-blue-500/30 rounded-full px-2 py-0.5 font-medium">
            Interactive Demo
          </span>
        </div>
        <div className="flex items-center gap-3">
          {events.length > 0 && (
            <>
              <span className="text-xs text-slate-500">
                {events.length} event{events.length !== 1 ? 's' : ''} captured
              </span>
              <button
                onClick={handleClear}
                className="flex items-center gap-1.5 text-xs text-slate-500 hover:text-red-400 transition-colors px-2 py-1 rounded hover:bg-slate-800"
              >
                <Trash2 className="w-3 h-3" /> Clear
              </button>
            </>
          )}
        </div>
      </header>

      {/* Main layout */}
      <div className="flex flex-1 min-h-0">
        {/* Left: demo scenarios */}
        <div className="w-[340px] shrink-0 border-r border-slate-800 overflow-y-auto">
          <DemoScenarios onRun={() => setSelectedIndex(null)} />
        </div>

        {/* Right: captured events — the star of the show */}
        <div className="flex-1 flex flex-col min-w-0">
          <CapturedEventsPanel
            events={events}
            selectedIndex={selectedIndex}
            onSelect={setSelectedIndex}
          />
        </div>
      </div>
    </div>
  );
}
