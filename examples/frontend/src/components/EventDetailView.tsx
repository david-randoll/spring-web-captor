import { useState } from 'react';
import type { HttpResponseEvent } from '../types/events';
import MethodBadge from './MethodBadge';
import StatusBadge from './StatusBadge';
import JsonViewer from './JsonViewer';
import { Clock, Globe, Monitor, MapPin, FileText, AlertTriangle, ArrowLeft, Layers, Server, ArrowDown, Search, GitBranch, Upload } from 'lucide-react';

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
    return <div className="text-xs text-slate-600 italic">{emptyMsg || 'None captured'}</div>;
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

function Section({ title, icon, children, accent }: { title: string; icon: React.ReactNode; children: React.ReactNode; accent?: string }) {
  return (
    <div className="space-y-2">
      <h3 className={`flex items-center gap-2 text-xs font-semibold uppercase tracking-wider ${accent || 'text-slate-500'}`}>
        {icon} {title}
      </h3>
      {children}
    </div>
  );
}

export default function EventDetailView({ event, onBack }: { event: HttpResponseEvent; onBack: () => void }) {
  const [tab, setTab] = useState<'visual' | 'raw'>('visual');
  const statusCode = event.responseStatus;
  const hasFiles = event.bodyPayload?.files && Object.keys(event.bodyPayload.files).length > 0;
  const hasError = event.errorDetail && Object.keys(event.errorDetail).length > 0;
  const hasQueryParams = event.queryParams && Object.keys(event.queryParams).length > 0;
  const hasPathParams = event.pathParams && Object.keys(event.pathParams).length > 0;
  const hasRequestBody = event.bodyPayload?.body != null;
  const hasResponseBody = event.responseBody != null;

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="px-5 py-3 border-b border-slate-800 flex items-center gap-3 shrink-0">
        <button onClick={onBack} className="p-1.5 hover:bg-slate-800 rounded-lg transition-colors">
          <ArrowLeft className="w-4 h-4 text-slate-400" />
        </button>
        <MethodBadge method={event.method} />
        <span className="font-mono text-sm text-slate-300 truncate flex-1">{event.path}</span>
        <StatusBadge status={statusCode} />
        <div className="flex gap-1">
          <button
            onClick={() => setTab('visual')}
            className={`px-2.5 py-1 rounded text-xs font-medium transition-colors ${tab === 'visual' ? 'bg-slate-800 text-blue-400' : 'text-slate-500 hover:text-slate-300'}`}
          >
            Visual
          </button>
          <button
            onClick={() => setTab('raw')}
            className={`px-2.5 py-1 rounded text-xs font-medium transition-colors ${tab === 'raw' ? 'bg-slate-800 text-blue-400' : 'text-slate-500 hover:text-slate-300'}`}
          >
            Raw JSON
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto">
        {tab === 'raw' ? (
          <div className="p-5">
            <JsonViewer data={event} label="Full Captured Event" />
          </div>
        ) : (
          <div className="p-5 space-y-6">
            {/* Summary cards */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
              <div className="bg-slate-900 rounded-xl p-3 border border-slate-800">
                <div className="text-[10px] text-slate-500 uppercase tracking-wider mb-1.5">Method</div>
                <MethodBadge method={event.method} />
              </div>
              <div className="bg-slate-900 rounded-xl p-3 border border-slate-800">
                <div className="text-[10px] text-slate-500 uppercase tracking-wider mb-1.5">Status</div>
                <StatusBadge status={statusCode} />
              </div>
              <div className="bg-slate-900 rounded-xl p-3 border border-slate-800">
                <div className="flex items-center gap-1 text-[10px] text-slate-500 uppercase tracking-wider mb-1.5">
                  <Clock className="w-3 h-3" /> Duration
                </div>
                <span className="font-mono text-sm text-slate-200 font-medium">{parseDuration(event.duration)}</span>
              </div>
              <div className="bg-slate-900 rounded-xl p-3 border border-slate-800">
                <div className="flex items-center gap-1 text-[10px] text-slate-500 uppercase tracking-wider mb-1.5">
                  <MapPin className="w-3 h-3" /> Endpoint Exists
                </div>
                <span className={`font-mono text-sm font-medium ${event.endpointExists ? 'text-emerald-400' : 'text-red-400'}`}>
                  {String(event.endpointExists)}
                </span>
              </div>
            </div>

            {/* Full URL */}
            <div className="bg-slate-900 rounded-xl p-3 border border-slate-800">
              <div className="flex items-center gap-1.5 text-[10px] text-slate-500 uppercase tracking-wider mb-1.5">
                <Globe className="w-3 h-3" /> Captured URL
              </div>
              <span className="font-mono text-sm text-slate-200 break-all">{event.fullUrl}</span>
            </div>

            {/* Metadata row */}
            {(event.userIp || event.userAgent) && (
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
                {event.userIp && (
                  <div className="bg-slate-900 rounded-xl p-3 border border-slate-800">
                    <div className="text-[10px] text-slate-500 uppercase tracking-wider mb-1.5">Client IP</div>
                    <span className="font-mono text-sm text-slate-200">{event.userIp}</span>
                  </div>
                )}
                {event.userAgent && (
                  <div className="bg-slate-900 rounded-xl p-3 border border-slate-800">
                    <div className="flex items-center gap-1 text-[10px] text-slate-500 uppercase tracking-wider mb-1.5">
                      <Monitor className="w-3 h-3" /> User Agent
                    </div>
                    <span className="font-mono text-xs text-slate-300 break-all">{event.userAgent}</span>
                  </div>
                )}
              </div>
            )}

            {/* Divider: Request section */}
            <div className="flex items-center gap-3 pt-2">
              <div className="h-px flex-1 bg-slate-800" />
              <span className="flex items-center gap-1.5 text-xs font-semibold text-blue-400 uppercase tracking-wider">
                <Layers className="w-3.5 h-3.5" /> Request Capture
              </span>
              <div className="h-px flex-1 bg-slate-800" />
            </div>

            {/* Request headers */}
            <Section title="Headers" icon={<Server className="w-3.5 h-3.5" />}>
              <DataTable data={event.headers} emptyMsg="No headers captured" />
            </Section>

            {/* Query params */}
            {hasQueryParams && (
              <Section title="Query Parameters" icon={<Search className="w-3.5 h-3.5" />}>
                <DataTable data={event.queryParams} />
              </Section>
            )}

            {/* Path params */}
            {hasPathParams && (
              <Section title="Path Parameters" icon={<GitBranch className="w-3.5 h-3.5" />}>
                <DataTable data={event.pathParams} />
              </Section>
            )}

            {/* Request body */}
            {hasRequestBody && (
              <Section title="Request Body" icon={<FileText className="w-3.5 h-3.5" />}>
                <JsonViewer data={event.bodyPayload!.body} />
              </Section>
            )}

            {/* Uploaded files */}
            {hasFiles && (
              <Section title="Uploaded Files" icon={<Upload className="w-3.5 h-3.5" />}>
                {Object.entries(event.bodyPayload!.files).map(([field, files]) =>
                  files.map((f, i) => (
                    <div key={`${field}-${i}`} className="bg-slate-900 border border-slate-800 rounded-xl p-3">
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
              </Section>
            )}

            {/* Divider: Response section */}
            <div className="flex items-center gap-3 pt-2">
              <div className="h-px flex-1 bg-slate-800" />
              <span className="flex items-center gap-1.5 text-xs font-semibold text-emerald-400 uppercase tracking-wider">
                <ArrowDown className="w-3.5 h-3.5" /> Response Capture
              </span>
              <div className="h-px flex-1 bg-slate-800" />
            </div>

            {/* Response status */}
            <div className="bg-slate-900 rounded-xl p-3 border border-slate-800">
              <div className="text-[10px] text-slate-500 uppercase tracking-wider mb-1.5">Response Status</div>
              <StatusBadge status={statusCode} />
            </div>

            {/* Response headers */}
            <Section title="Response Headers" icon={<Server className="w-3.5 h-3.5" />}>
              <DataTable data={event.responseHeaders} emptyMsg="No response headers captured" />
            </Section>

            {/* Error detail */}
            {hasError && (
              <Section title="Error Detail" icon={<AlertTriangle className="w-3.5 h-3.5" />} accent="text-red-400">
                <JsonViewer data={event.errorDetail} />
              </Section>
            )}

            {/* Response body */}
            {hasResponseBody && (
              <Section title="Response Body" icon={<FileText className="w-3.5 h-3.5" />} accent="text-emerald-400">
                <JsonViewer data={event.responseBody} />
              </Section>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

