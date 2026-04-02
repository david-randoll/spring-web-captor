import type { DemoScenario } from './demoData';
import MethodBadge from './MethodBadge';
import { Send } from 'lucide-react';

interface Props {
  demo: DemoScenario;
  onSend: () => void;
}

export default function RequestPreview({ demo, onSend }: Props) {
  const urlParts = demo.url.split('?');
  const path = urlParts[0];
  const queryString = urlParts[1] || null;

  return (
    <div className="max-w-3xl mx-auto">
      {/* Description */}
      <div className="text-center mb-5 sm:mb-8">
        <h2 className="text-lg sm:text-xl font-semibold text-slate-100 mb-2">{demo.title}</h2>
        <p className="text-xs sm:text-sm text-slate-500">{demo.description}</p>
      </div>

      {/* Request card */}
      <div className="bg-slate-900/70 border border-slate-800 rounded-xl sm:rounded-2xl overflow-hidden">
        <div className="px-3 sm:px-5 py-2.5 sm:py-3 border-b border-slate-800/50 flex items-center justify-between gap-2">
          <span className="text-[10px] sm:text-xs font-semibold text-slate-500 uppercase tracking-wider shrink-0">HTTP Request</span>
          <div className="flex flex-wrap justify-end gap-1">
            {demo.tags.map((tag) => (
              <span key={tag} className="text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-500 font-medium">
                {tag}
              </span>
            ))}
          </div>
        </div>

        <div className="p-3 sm:p-5 font-mono text-xs sm:text-sm space-y-3">
          {/* Method + URL line */}
          <div className="flex items-start gap-2">
            <MethodBadge method={demo.method} />
            <div className="text-slate-300 break-all min-w-0">
              <span className="text-slate-500 hidden sm:inline">localhost:11006</span>
              <span className="text-slate-200">{path}</span>
              {queryString && (
                <span className="text-amber-400/70">?{queryString}</span>
              )}
            </div>
          </div>

          {/* Headers */}
          {Object.keys(demo.headers).length > 0 && (
            <div className="pt-2 border-t border-slate-800/50">
              <div className="text-[10px] text-slate-600 uppercase tracking-wider mb-1.5">Headers</div>
              {Object.entries(demo.headers).map(([key, value]) => (
                <div key={key} className="text-[11px] sm:text-xs break-all">
                  <span className="text-sky-400">{key}</span>
                  <span className="text-slate-600">: </span>
                  <span className="text-slate-400">{value}</span>
                </div>
              ))}
            </div>
          )}

          {/* Body */}
          {demo.body && (
            <div className="pt-2 border-t border-slate-800/50">
              <div className="text-[10px] text-slate-600 uppercase tracking-wider mb-1.5">Body</div>
              <pre className="text-[11px] sm:text-xs text-emerald-400/80 whitespace-pre-wrap bg-slate-950/50 rounded-lg p-2 sm:p-3 max-h-48 overflow-auto break-all">
                {demo.body}
              </pre>
            </div>
          )}
        </div>
      </div>

      {/* Send button */}
      <div className="flex justify-center mt-6 sm:mt-8">
        <button
          onClick={onSend}
          className="group flex items-center gap-3 bg-blue-600 hover:bg-blue-500 text-white rounded-xl px-6 sm:px-8 py-3 text-sm font-semibold transition-all hover:scale-105 hover:shadow-lg hover:shadow-blue-500/20"
        >
          <Send className="w-4 h-4 transition-transform group-hover:translate-x-0.5" />
          Send & Capture
        </button>
      </div>
    </div>
  );
}
