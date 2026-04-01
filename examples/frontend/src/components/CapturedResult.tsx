import { useState, useEffect } from 'react';
import type { HttpResponseEvent } from '../types/events';
import type { DemoScenario } from './demoData';
import MethodBadge from './MethodBadge';
import StatusBadge from './StatusBadge';
import JsonViewer from './JsonViewer';
import {
  Clock, Globe, Monitor, MapPin, FileText, AlertTriangle,
  Server, ArrowDown, Layers, Search, GitBranch, Upload,
  RotateCcw, Sparkles, Puzzle,
} from 'lucide-react';

function parseDuration(d: unknown): string {
  if (!d) return '-';
  const s = String(d);
  const match = /PT(?:(\d+)H)?(?:(\d+)M)?(?:([\d.]+)S)?/.exec(s);
  if (match) {
    const h = Number.parseInt(match[1] || '0');
    const m = Number.parseInt(match[2] || '0');
    const sec = Number.parseFloat(match[3] || '0');
    const totalMs = (h * 3600 + m * 60 + sec) * 1000;
    if (totalMs < 1000) return `${Math.round(totalMs)}ms`;
    return `${(totalMs / 1000).toFixed(2)}s`;
  }
  if (typeof d === 'number') return `${d}ms`;
  return s;
}

function DataTable({ data, emptyMsg }: { data: Record<string, unknown> | null | undefined; emptyMsg?: string }) {
  if (!data || Object.keys(data).length === 0) {
    return <div className="text-xs text-slate-600 italic">{emptyMsg || 'None'}</div>;
  }
  return (
    <div className="border border-slate-800 rounded-lg overflow-hidden">
      <table className="w-full text-xs">
        <tbody>
          {Object.entries(data).map(([key, val]) => (
            <tr key={key} className="border-b border-slate-800/50 last:border-0">
              <td className="px-3 py-1.5 font-mono text-sky-400 bg-slate-900/50 w-1/3 whitespace-nowrap align-top">{key}</td>
              <td className="px-3 py-1.5 font-mono text-slate-300 break-all">
                {Array.isArray(val) ? val.join(', ') : String(val ?? '')}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function FadeInSection({ children, delay = 0 }: { children: React.ReactNode; delay?: number }) {
  const [visible, setVisible] = useState(false);
  useEffect(() => {
    const timer = setTimeout(() => setVisible(true), delay);
    return () => clearTimeout(timer);
  }, [delay]);
  return (
    <div className={`transition-all duration-500 ${visible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4'}`}>
      {children}
    </div>
  );
}

function SectionHeader({ title, icon, accent }: { title: string; icon: React.ReactNode; accent?: string }) {
  return (
    <h3 className={`flex items-center gap-2 text-xs font-semibold uppercase tracking-wider mb-2 ${accent || 'text-slate-500'}`}>
      {icon} {title}
    </h3>
  );
}

interface Props {
  event: HttpResponseEvent;
  demo: DemoScenario;
  onTryAnother: () => void;
}

export default function CapturedResult({ event, demo, onTryAnother }: Props) {
  const [tab, setTab] = useState<'visual' | 'raw'>('visual');
  const hasFiles = event.bodyPayload?.files && Object.keys(event.bodyPayload.files).length > 0;
  const hasError = event.errorDetail && Object.keys(event.errorDetail).length > 0;
  const hasQueryParams = event.queryParams && Object.keys(event.queryParams).length > 0;
  const hasPathParams = event.pathParams && Object.keys(event.pathParams).length > 0;
  const hasRequestBody = event.bodyPayload?.body != null;
  const hasResponseBody = event.responseBody != null;

  let sectionDelay = 0;
  const nextDelay = () => {
    sectionDelay += 150;
    return sectionDelay;
  };

  return (
    <div className="max-w-4xl mx-auto">
      {/* Header */}
      <FadeInSection>
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <Sparkles className="w-5 h-5 text-blue-400" />
            <h2 className="text-lg font-semibold text-slate-100">Captured by Spring Web Captor</h2>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setTab(tab === 'visual' ? 'raw' : 'visual')}
              className="text-xs text-slate-500 hover:text-slate-300 px-3 py-1.5 rounded-lg border border-slate-800 hover:border-slate-700 transition-colors"
            >
              {tab === 'visual' ? 'View Raw JSON' : 'Visual View'}
            </button>
            <button
              onClick={onTryAnother}
              className="flex items-center gap-1.5 text-xs text-slate-500 hover:text-blue-400 px-3 py-1.5 rounded-lg border border-slate-800 hover:border-blue-500/30 transition-colors"
            >
              <RotateCcw className="w-3 h-3" /> Try Again
            </button>
          </div>
        </div>
      </FadeInSection>

      {tab === 'raw' ? (
        <FadeInSection>
          <JsonViewer data={event} label="Full HttpResponseEvent" />
        </FadeInSection>
      ) : (
        <div className="space-y-5">
          {/* Summary cards */}
          <FadeInSection delay={nextDelay()}>
            <div className="grid grid-cols-2 lg:grid-cols-3 gap-3">
              <div className="bg-slate-900 rounded-xl p-4 border border-slate-800">
                <div className="text-[10px] text-slate-500 uppercase tracking-wider mb-2">Method</div>
                <MethodBadge method={event.method} />
              </div>
              <div className="bg-slate-900 rounded-xl p-4 border border-slate-800">
                <div className="text-[10px] text-slate-500 uppercase tracking-wider mb-2">Status</div>
                <StatusBadge status={event.responseStatus} />
              </div>
              <div className="bg-slate-900 rounded-xl p-4 border border-slate-800">
                <div className="flex items-center gap-1 text-[10px] text-slate-500 uppercase tracking-wider mb-2">
                  <MapPin className="w-3 h-3" /> Endpoint Exists
                </div>
                <span className={`font-mono text-sm font-medium ${event.endpointExists ? 'text-emerald-400' : 'text-red-400'}`}>
                  {String(event.endpointExists)}
                </span>
              </div>
            </div>
          </FadeInSection>

          {/* Full URL */}
          <FadeInSection delay={nextDelay()}>
            <div className="bg-slate-900 rounded-xl p-4 border border-slate-800">
              <div className="flex items-center gap-1.5 text-[10px] text-slate-500 uppercase tracking-wider mb-2">
                <Globe className="w-3 h-3" /> Captured Full URL
              </div>
              <span className="font-mono text-sm text-slate-200 break-all">{event.fullUrl}</span>
            </div>
          </FadeInSection>

          {/* === EXTENSIONS === */}
          {(event.duration || event.userIp || event.userAgent) && (
            <>
              <FadeInSection delay={nextDelay()}>
                <div className="flex items-center gap-3 pt-3">
                  <div className="h-px flex-1 bg-gradient-to-r from-transparent to-violet-500/20" />
                  <span className="flex items-center gap-1.5 text-xs font-semibold text-violet-400 uppercase tracking-wider">
                    <Puzzle className="w-3.5 h-3.5" /> Built-in Extensions
                  </span>
                  <div className="h-px flex-1 bg-gradient-to-l from-transparent to-violet-500/20" />
                </div>
                <p className="text-[11px] text-slate-600 text-center mt-1">
                  Extensions enrich events with additional data via the <span className="text-violet-400/70 font-mono">IHttpEventExtension</span> interface. These are built-in — you can add your own.
                </p>
              </FadeInSection>

              <FadeInSection delay={nextDelay()}>
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-3">
                  {event.duration && (
                    <div className="bg-slate-900 rounded-xl p-4 border border-violet-500/20">
                      <div className="flex items-center justify-between mb-1">
                        <div className="flex items-center gap-1 text-[10px] text-violet-400 uppercase tracking-wider font-semibold">
                          <Clock className="w-3 h-3" /> Duration
                        </div>
                        <span className="text-[9px] bg-violet-500/10 text-violet-400 border border-violet-500/20 rounded-full px-1.5 py-0.5 font-medium">
                          Extension
                        </span>
                      </div>
                      <span className="font-mono text-sm text-slate-200 font-medium">{parseDuration(event.duration)}</span>
                      {event.startTime && event.endTime && (
                        <div className="text-[10px] text-slate-600 font-mono mt-1">
                          {event.startTime} → {event.endTime}
                        </div>
                      )}
                    </div>
                  )}
                  {event.userIp && (
                    <div className="bg-slate-900 rounded-xl p-4 border border-violet-500/20">
                      <div className="flex items-center justify-between mb-1">
                        <div className="text-[10px] text-violet-400 uppercase tracking-wider font-semibold">Client IP</div>
                        <span className="text-[9px] bg-violet-500/10 text-violet-400 border border-violet-500/20 rounded-full px-1.5 py-0.5 font-medium">
                          Extension
                        </span>
                      </div>
                      <span className="font-mono text-sm text-slate-200">{event.userIp}</span>
                    </div>
                  )}
                  {event.userAgent && (
                    <div className="bg-slate-900 rounded-xl p-4 border border-violet-500/20">
                      <div className="flex items-center justify-between mb-1">
                        <div className="flex items-center gap-1 text-[10px] text-violet-400 uppercase tracking-wider font-semibold">
                          <Monitor className="w-3 h-3" /> User Agent
                        </div>
                        <span className="text-[9px] bg-violet-500/10 text-violet-400 border border-violet-500/20 rounded-full px-1.5 py-0.5 font-medium">
                          Extension
                        </span>
                      </div>
                      <span className="font-mono text-xs text-slate-300 break-all">{event.userAgent}</span>
                    </div>
                  )}
                </div>
              </FadeInSection>
            </>
          )}

          {/* === REQUEST CAPTURE === */}
          <FadeInSection delay={nextDelay()}>
            <div className="flex items-center gap-3 pt-3">
              <div className="h-px flex-1 bg-gradient-to-r from-transparent to-blue-500/20" />
              <span className="flex items-center gap-1.5 text-xs font-semibold text-blue-400 uppercase tracking-wider">
                <Layers className="w-3.5 h-3.5" /> Request Capture
              </span>
              <div className="h-px flex-1 bg-gradient-to-l from-transparent to-blue-500/20" />
            </div>
          </FadeInSection>

          <FadeInSection delay={nextDelay()}>
            <SectionHeader title="Request Headers" icon={<Server className="w-3.5 h-3.5" />} />
            <DataTable data={event.headers} emptyMsg="No headers captured" />
          </FadeInSection>

          {hasQueryParams && (
            <FadeInSection delay={nextDelay()}>
              <SectionHeader title="Query Parameters" icon={<Search className="w-3.5 h-3.5" />} accent="text-amber-400" />
              <DataTable data={event.queryParams} />
            </FadeInSection>
          )}

          {hasPathParams && (
            <FadeInSection delay={nextDelay()}>
              <SectionHeader title="Path Parameters" icon={<GitBranch className="w-3.5 h-3.5" />} accent="text-purple-400" />
              <DataTable data={event.pathParams} />
            </FadeInSection>
          )}

          {hasRequestBody && (
            <FadeInSection delay={nextDelay()}>
              <SectionHeader title="Request Body" icon={<FileText className="w-3.5 h-3.5" />} />
              <JsonViewer data={event.bodyPayload!.body} />
            </FadeInSection>
          )}

          {hasFiles && (
            <FadeInSection delay={nextDelay()}>
              <SectionHeader title="Uploaded Files" icon={<Upload className="w-3.5 h-3.5" />} accent="text-cyan-400" />
              {Object.entries(event.bodyPayload!.files).map(([field, files]) =>
                files.map((f, i) => (
                  <div key={`${field}-${i}`} className="bg-slate-900 border border-slate-800 rounded-xl p-4 mb-2">
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-mono text-sm text-sky-400 font-medium">{f.filename}</span>
                      <span className="text-xs text-slate-500">{(f.size / 1024).toFixed(1)} KB</span>
                    </div>
                    <div className="text-xs text-slate-500">
                      Field: <span className="text-slate-400">{field}</span> | Type: <span className="text-slate-400">{f.contentType}</span>
                    </div>
                    {f.contentType?.startsWith('image/') && f.base64Content && (
                      <img
                        src={`data:${f.contentType};base64,${f.base64Content}`}
                        alt={f.filename}
                        className="mt-2 max-h-32 rounded border border-slate-700"
                      />
                    )}
                  </div>
                ))
              )}
            </FadeInSection>
          )}

          {/* === RESPONSE CAPTURE === */}
          <FadeInSection delay={nextDelay()}>
            <div className="flex items-center gap-3 pt-3">
              <div className="h-px flex-1 bg-gradient-to-r from-transparent to-emerald-500/20" />
              <span className="flex items-center gap-1.5 text-xs font-semibold text-emerald-400 uppercase tracking-wider">
                <ArrowDown className="w-3.5 h-3.5" /> Response Capture
              </span>
              <div className="h-px flex-1 bg-gradient-to-l from-transparent to-emerald-500/20" />
            </div>
          </FadeInSection>

          <FadeInSection delay={nextDelay()}>
            <div className="bg-slate-900 rounded-xl p-4 border border-slate-800">
              <div className="text-[10px] text-slate-500 uppercase tracking-wider mb-2">Response Status</div>
              <StatusBadge status={event.responseStatus} />
            </div>
          </FadeInSection>

          <FadeInSection delay={nextDelay()}>
            <SectionHeader title="Response Headers" icon={<Server className="w-3.5 h-3.5" />} accent="text-emerald-500" />
            <DataTable data={event.responseHeaders} emptyMsg="No response headers captured" />
          </FadeInSection>

          {hasError && (
            <FadeInSection delay={nextDelay()}>
              <SectionHeader title="Error Detail" icon={<AlertTriangle className="w-3.5 h-3.5" />} accent="text-red-400" />
              <JsonViewer data={event.errorDetail} />
            </FadeInSection>
          )}

          {hasResponseBody && (
            <FadeInSection delay={nextDelay()}>
              <SectionHeader title="Response Body" icon={<FileText className="w-3.5 h-3.5" />} accent="text-emerald-400" />
              <JsonViewer data={event.responseBody} />
            </FadeInSection>
          )}

          {/* Bottom padding */}
          <div className="h-8" />
        </div>
      )}
    </div>
  );
}
