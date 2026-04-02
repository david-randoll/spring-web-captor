import { useState, useCallback } from 'react';
import type { HttpResponseEvent } from './types/events';
import { fetchCapturedEvents, clearCapturedEvents } from './api/client';
import DemoSelector from './components/DemoSelector';
import RequestPreview from './components/RequestPreview';
import CaptureAnimation from './components/CaptureAnimation';
import CapturedResult from './components/CapturedResult';
import { Radio } from 'lucide-react';
import type { DemoScenario } from './components/demoData';
import { DEMOS } from './components/demoData';

type Phase = 'idle' | 'previewing' | 'sending' | 'captured';

export default function App() {
  const [selectedDemo, setSelectedDemo] = useState<DemoScenario>(DEMOS[0]);
  const [phase, setPhase] = useState<Phase>('idle');
  const [capturedEvent, setCapturedEvent] = useState<HttpResponseEvent | null>(null);

  const handleSelectDemo = (demo: DemoScenario) => {
    setSelectedDemo(demo);
    setPhase('previewing');
    setCapturedEvent(null);
  };

  const handleSend = useCallback(async () => {
    setPhase('sending');
    setCapturedEvent(null);
    await clearCapturedEvents();

    // Small delay so the clear takes effect
    await new Promise((r) => setTimeout(r, 200));

    // Execute the request
    await selectedDemo.run().catch(() => {});

    // Wait a beat for the backend to publish the event
    await new Promise((r) => setTimeout(r, 600));

    // Fetch the captured event
    const data = await fetchCapturedEvents();
    const events: HttpResponseEvent[] = data.responseEvents || [];
    if (events.length > 0) {
      setCapturedEvent(events[events.length - 1]);
    }
    setPhase('captured');
  }, [selectedDemo]);

  const handleReset = () => {
    setPhase('previewing');
    setCapturedEvent(null);
  };

  return (
    <div className="min-h-screen bg-[#0a0a0f] text-slate-200">
      {/* Header */}
      <header className="border-b border-slate-800">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 h-14 flex items-center gap-2 sm:gap-3">
          <Radio className="w-5 h-5 text-blue-400" />
          <h1 className="text-sm sm:text-base font-bold tracking-tight">Spring Web Captor</h1>
          <span className="text-[11px] bg-blue-500/15 text-blue-400 border border-blue-500/30 rounded-full px-2 py-0.5 font-medium">
            Interactive Demo
          </span>
        </div>
      </header>

      {/* Demo selector */}
      <div className="border-b border-slate-800 bg-slate-950/50">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 py-3 sm:py-4">
          <DemoSelector demos={DEMOS} selected={selectedDemo} onSelect={handleSelectDemo} />
        </div>
      </div>

      {/* Main content */}
      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-5 sm:py-8">
        {phase === 'idle' && (
          <div className="text-center py-20">
            <p className="text-slate-500 text-lg">Select a demo above to see the library in action</p>
          </div>
        )}

        {phase === 'previewing' && (
          <RequestPreview demo={selectedDemo} onSend={handleSend} />
        )}

        {phase === 'sending' && (
          <CaptureAnimation demo={selectedDemo} />
        )}

        {phase === 'captured' && capturedEvent && (
          <CapturedResult event={capturedEvent} demo={selectedDemo} onTryAnother={handleReset} />
        )}
      </div>
    </div>
  );
}
