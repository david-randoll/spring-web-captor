import { useState } from 'react';
import { useCapturedEvents } from './hooks/useCapturedEvents';
import ApiPlayground from './components/ApiPlayground';
import CapturedEventsPanel from './components/CapturedEventsPanel';
import PreBuiltDemos from './components/PreBuiltDemos';
import ConfigViewer from './components/ConfigViewer';
import { Terminal, Rocket, Settings, Radio } from 'lucide-react';

type View = 'playground' | 'demos' | 'config';

export default function App() {
  const [view, setView] = useState<View>('playground');
  const { events, clear } = useCapturedEvents();

  const navItems: { key: View; label: string; icon: React.ReactNode }[] = [
    { key: 'playground', label: 'API Playground', icon: <Terminal className="w-4 h-4" /> },
    { key: 'demos', label: 'Quick Demos', icon: <Rocket className="w-4 h-4" /> },
    { key: 'config', label: 'Configuration', icon: <Settings className="w-4 h-4" /> },
  ];

  return (
    <div className="h-screen flex flex-col bg-[#0a0a0f]">
      {/* Top bar */}
      <header className="h-12 border-b border-slate-800 flex items-center justify-between px-4 shrink-0">
        <div className="flex items-center gap-3">
          <Radio className="w-5 h-5 text-blue-400" />
          <h1 className="text-sm font-bold text-slate-200 tracking-tight">Spring Web Captor</h1>
          <span className="text-xs bg-blue-500/15 text-blue-400 border border-blue-500/30 rounded-full px-2 py-0">Demo</span>
        </div>
        <div className="flex items-center gap-1">
          {navItems.map((item) => (
            <button
              key={item.key}
              onClick={() => setView(item.key)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                view === item.key
                  ? 'bg-slate-800 text-blue-400'
                  : 'text-slate-500 hover:text-slate-300 hover:bg-slate-800/50'
              }`}
            >
              {item.icon}
              {item.label}
            </button>
          ))}
        </div>
      </header>

      {/* Main content */}
      <div className="flex flex-1 min-h-0">
        {/* Left panel */}
        <div className="flex-1 border-r border-slate-800 flex flex-col min-w-0">
          {view === 'playground' && <ApiPlayground />}
          {view === 'demos' && <PreBuiltDemos />}
          {view === 'config' && <ConfigViewer />}
        </div>

        {/* Right panel — captured events */}
        <div className="w-[480px] shrink-0 flex flex-col bg-slate-900/30">
          <CapturedEventsPanel events={events} onClear={clear} />
        </div>
      </div>
    </div>
  );
}
