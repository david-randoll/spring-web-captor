import { useState } from 'react';
import type { HttpResponseEvent } from '../types/events';
import MethodBadge from './MethodBadge';
import StatusBadge from './StatusBadge';
import JsonViewer from './JsonViewer';
import { Clock, Globe, Monitor, MapPin, FileText, AlertTriangle, ArrowLeft } from 'lucide-react';

type Tab = 'overview' | 'request' | 'response' | 'raw';

function parseDuration(d: unknown): string {
  if (!d) return '-';
  const s = String(d);
  // ISO-8601 duration: PT0.123S or PT3.045S
  const match = s.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:([\d.]+)S)?/);
  if (match) {
    const h = parseInt(match[1] || '0');
    const m = parseInt(match[2] || '0');
    const sec = parseFloat(match[3] || '0');
    const totalMs = (h * 3600 + m * 60 + sec) * 1000;
    if (totalMs < 1000) return `${Math.round(totalMs)}ms`;
    return `${(totalMs / 1000).toFixed(2)}s`;
  }
  // Might be a number in nanos or millis
  if (typeof d === 'number') return `${d}ms`;
  return s;
}

function DataTable({ data, emptyMsg }: { data: Record<string, unknown> | null | undefined; emptyMsg?: string }) {
  if (!data || Object.keys(data).length === 0) {
    return <div className="text-sm text-slate-500 italic">{emptyMsg || 'None'}</div>;
  }
  return (
    <div className="border border-slate-800 rounded-lg overflow-hidden">
      <table className="w-full text-sm">
        <tbody>
          {Object.entries(data).map(([key, val]) => (
            <tr key={key} className="border-b border-slate-800 last:border-0">
              <td className="px-3 py-1.5 font-mono text-sky-400 bg-slate-900/50 w-1/3 whitespace-nowrap">{key}</td>
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

export default function EventDetailView({ event, onBack }: { event: HttpResponseEvent; onBack: () => void }) {
  const [tab, setTab] = useState<Tab>('overview');

  const tabs: { key: Tab; label: string }[] = [
    { key: 'overview', label: 'Overview' },
    { key: 'request', label: 'Request' },
    { key: 'response', label: 'Response' },
    { key: 'raw', label: 'Raw JSON' },
  ];

  const statusCode = event.responseStatus;
  const hasFiles = event.bodyPayload?.files && Object.keys(event.bodyPayload.files).length > 0;

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="px-4 py-3 border-b border-slate-800 flex items-center gap-3">
        <button onClick={onBack} className="p-1 hover:bg-slate-800 rounded transition-colors">
          <ArrowLeft className="w-4 h-4 text-slate-400" />
        </button>
        <MethodBadge method={event.method} />
        <span className="font-mono text-sm text-slate-300 truncate">{event.path}</span>
        <StatusBadge status={statusCode} />
      </div>

      {/* Tabs */}
      <div className="flex border-b border-slate-800 px-4">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`px-3 py-2 text-sm font-medium border-b-2 transition-colors ${
              tab === t.key
                ? 'border-blue-500 text-blue-400'
                : 'border-transparent text-slate-500 hover:text-slate-300'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-4 space-y-4">
        {tab === 'overview' && (
          <>
            <div className="grid grid-cols-2 gap-3">
              <div className="bg-slate-900 rounded-lg p-3 border border-slate-800">
                <div className="text-xs text-slate-500 uppercase tracking-wide mb-1">Method</div>
                <MethodBadge method={event.method} />
              </div>
              <div className="bg-slate-900 rounded-lg p-3 border border-slate-800">
                <div className="text-xs text-slate-500 uppercase tracking-wide mb-1">Status</div>
                <StatusBadge status={statusCode} />
              </div>
              <div className="bg-slate-900 rounded-lg p-3 border border-slate-800">
                <div className="flex items-center gap-1.5 text-xs text-slate-500 uppercase tracking-wide mb-1">
                  <Clock className="w-3 h-3" /> Duration
                </div>
                <span className="font-mono text-sm text-slate-200">{parseDuration(event.duration)}</span>
              </div>
              <div className="bg-slate-900 rounded-lg p-3 border border-slate-800">
                <div className="flex items-center gap-1.5 text-xs text-slate-500 uppercase tracking-wide mb-1">
                  <MapPin className="w-3 h-3" /> Endpoint Exists
                </div>
                <span className={`font-mono text-sm ${event.endpointExists ? 'text-emerald-400' : 'text-red-400'}`}>
                  {String(event.endpointExists)}
                </span>
              </div>
            </div>

            <div className="bg-slate-900 rounded-lg p-3 border border-slate-800">
              <div className="flex items-center gap-1.5 text-xs text-slate-500 uppercase tracking-wide mb-1">
                <Globe className="w-3 h-3" /> Full URL
              </div>
              <span className="font-mono text-sm text-slate-200 break-all">{event.fullUrl}</span>
            </div>

            {event.userIp && (
              <div className="bg-slate-900 rounded-lg p-3 border border-slate-800">
                <div className="text-xs text-slate-500 uppercase tracking-wide mb-1">Client IP</div>
                <span className="font-mono text-sm text-slate-200">{event.userIp}</span>
              </div>
            )}

            {event.userAgent && (
              <div className="bg-slate-900 rounded-lg p-3 border border-slate-800">
                <div className="flex items-center gap-1.5 text-xs text-slate-500 uppercase tracking-wide mb-1">
                  <Monitor className="w-3 h-3" /> User Agent
                </div>
                <span className="font-mono text-xs text-slate-300 break-all">{event.userAgent}</span>
              </div>
            )}
          </>
        )}

        {tab === 'request' && (
          <>
            <div>
              <h4 className="text-sm font-medium text-slate-300 mb-2">Headers</h4>
              <DataTable data={event.headers} emptyMsg="No headers captured" />
            </div>
            <div>
              <h4 className="text-sm font-medium text-slate-300 mb-2">Query Parameters</h4>
              <DataTable data={event.queryParams} emptyMsg="No query parameters" />
            </div>
            <div>
              <h4 className="text-sm font-medium text-slate-300 mb-2">Path Parameters</h4>
              <DataTable data={event.pathParams} emptyMsg="No path parameters" />
            </div>
            {event.bodyPayload?.body != null && (
              <div>
                <h4 className="text-sm font-medium text-slate-300 mb-2">Request Body</h4>
                <JsonViewer data={event.bodyPayload.body} />
              </div>
            )}
            {hasFiles && (
              <div>
                <h4 className="flex items-center gap-1.5 text-sm font-medium text-slate-300 mb-2">
                  <FileText className="w-4 h-4" /> Uploaded Files
                </h4>
                {Object.entries(event.bodyPayload!.files).map(([field, files]) =>
                  files.map((f, i) => (
                    <div key={`${field}-${i}`} className="bg-slate-900 border border-slate-800 rounded-lg p-3 mb-2">
                      <div className="flex items-center justify-between mb-2">
                        <span className="font-mono text-sm text-sky-400">{f.filename}</span>
                        <span className="text-xs text-slate-500">{(f.size / 1024).toFixed(1)} KB</span>
                      </div>
                      <div className="text-xs text-slate-500">
                        Field: {field} | Type: {f.contentType}
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
              </div>
            )}
          </>
        )}

        {tab === 'response' && (
          <>
            <div className="flex items-center gap-3 mb-2">
              <StatusBadge status={statusCode} />
            </div>
            <div>
              <h4 className="text-sm font-medium text-slate-300 mb-2">Response Headers</h4>
              <DataTable data={event.responseHeaders} emptyMsg="No response headers" />
            </div>
            {event.errorDetail && (
              <div>
                <h4 className="flex items-center gap-1.5 text-sm font-medium text-red-400 mb-2">
                  <AlertTriangle className="w-4 h-4" /> Error Detail
                </h4>
                <JsonViewer data={event.errorDetail} />
              </div>
            )}
            {event.responseBody != null && (
              <div>
                <h4 className="text-sm font-medium text-slate-300 mb-2">Response Body</h4>
                <JsonViewer data={event.responseBody} />
              </div>
            )}
          </>
        )}

        {tab === 'raw' && (
          <JsonViewer data={event} label="Full Captured Event" />
        )}
      </div>
    </div>
  );
}
