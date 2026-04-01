import { useState, useRef } from 'react';
import { sendRequest } from '../api/client';
import MethodBadge from './MethodBadge';
import StatusBadge from './StatusBadge';
import JsonViewer from './JsonViewer';
import KeyValueEditor from './KeyValueEditor';
import { Send, Loader2, ChevronDown } from 'lucide-react';

const METHODS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'];

const CONTENT_TYPES = [
  { label: 'None', value: '' },
  { label: 'JSON', value: 'application/json' },
  { label: 'Form URL-encoded', value: 'application/x-www-form-urlencoded' },
  { label: 'Plain Text', value: 'text/plain' },
  { label: 'Multipart', value: 'multipart/form-data' },
];

const ENDPOINT_SUGGESTIONS = [
  { method: 'GET', path: '/demo/items', label: 'List items' },
  { method: 'GET', path: '/demo/items/1', label: 'Get item by ID' },
  { method: 'POST', path: '/demo/items', label: 'Create item' },
  { method: 'PUT', path: '/demo/items/1', label: 'Update item' },
  { method: 'PATCH', path: '/demo/items/1', label: 'Partial update' },
  { method: 'DELETE', path: '/demo/items/1', label: 'Delete item' },
  { method: 'POST', path: '/demo/form', label: 'Form submission' },
  { method: 'POST', path: '/demo/upload', label: 'File upload' },
  { method: 'POST', path: '/demo/text', label: 'Text echo' },
  { method: 'GET', path: '/demo/paths/electronics/42', label: 'Path variables' },
  { method: 'GET', path: '/demo/paths/wildcard/a/b/c/d', label: 'Catch-all wildcard' },
  { method: 'GET', path: '/demo/search?q=spring&page=1&size=10&filters=active&filters=premium', label: 'Search with params' },
  { method: 'GET', path: '/demo/errors/400', label: '400 Bad Request' },
  { method: 'GET', path: '/demo/errors/404', label: '404 Not Found' },
  { method: 'GET', path: '/demo/errors/500', label: '500 Server Error' },
  { method: 'GET', path: '/demo/errors/418', label: '418 Teapot' },
  { method: 'POST', path: '/demo/errors/validation', label: 'Validation error' },
  { method: 'GET', path: '/demo/slow/3', label: 'Slow request (3s)' },
];

interface KeyValue {
  key: string;
  value: string;
}

type Tab = 'headers' | 'params' | 'body';

export default function ApiPlayground() {
  const [method, setMethod] = useState('GET');
  const [path, setPath] = useState('/demo/items');
  const [tab, setTab] = useState<Tab>('body');
  const [headers, setHeaders] = useState<KeyValue[]>([]);
  const [contentType, setContentType] = useState('');
  const [bodyText, setBodyText] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [formFields, setFormFields] = useState<KeyValue[]>([{ key: '', value: '' }]);
  const [loading, setLoading] = useState(false);
  const [response, setResponse] = useState<{ status: number; statusText: string; headers: Record<string, string>; body: string } | null>(null);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const filteredSuggestions = ENDPOINT_SUGGESTIONS.filter(
    (s) => s.path.includes(path) || s.label.toLowerCase().includes(path.toLowerCase())
  );

  const handleSend = async () => {
    setLoading(true);
    setResponse(null);
    try {
      const reqHeaders: Record<string, string> = {};
      headers.forEach((h) => { if (h.key) reqHeaders[h.key] = h.value; });

      let body: string | FormData | undefined;

      if (contentType === 'application/json') {
        reqHeaders['Content-Type'] = 'application/json';
        body = bodyText;
      } else if (contentType === 'application/x-www-form-urlencoded') {
        reqHeaders['Content-Type'] = 'application/x-www-form-urlencoded';
        const params = new URLSearchParams();
        formFields.forEach((f) => { if (f.key) params.append(f.key, f.value); });
        body = params.toString();
      } else if (contentType === 'text/plain') {
        reqHeaders['Content-Type'] = 'text/plain';
        body = bodyText;
      } else if (contentType === 'multipart/form-data') {
        const fd = new FormData();
        formFields.forEach((f) => { if (f.key) fd.append(f.key, f.value); });
        if (file) fd.append('file', file);
        body = fd;
      }

      const res = await sendRequest(method, path, { headers: reqHeaders, body });
      setResponse(res);
    } catch (err) {
      setResponse({ status: 0, statusText: 'Network Error', headers: {}, body: String(err) });
    } finally {
      setLoading(false);
    }
  };

  const selectSuggestion = (s: typeof ENDPOINT_SUGGESTIONS[0]) => {
    setMethod(s.method);
    setPath(s.path);
    setShowSuggestions(false);

    // Auto-set content type and body for known endpoints
    if (s.path === '/demo/items' && s.method === 'POST') {
      setContentType('application/json');
      setBodyText(JSON.stringify({ name: 'New Gadget', description: 'A cool new gadget', tags: ['demo', 'new'] }, null, 2));
      setTab('body');
    } else if (s.path === '/demo/items/1' && s.method === 'PUT') {
      setContentType('application/json');
      setBodyText(JSON.stringify({ name: 'Updated Laptop', description: 'Updated description', tags: ['updated'] }, null, 2));
      setTab('body');
    } else if (s.path === '/demo/items/1' && s.method === 'PATCH') {
      setContentType('application/json');
      setBodyText(JSON.stringify({ name: 'Renamed Item' }, null, 2));
      setTab('body');
    } else if (s.path === '/demo/form') {
      setContentType('application/x-www-form-urlencoded');
      setFormFields([{ key: 'username', value: 'john_doe' }, { key: 'email', value: 'john@example.com' }]);
      setTab('body');
    } else if (s.path === '/demo/upload') {
      setContentType('multipart/form-data');
      setFormFields([{ key: 'description', value: 'Demo file upload' }]);
      setTab('body');
    } else if (s.path === '/demo/text') {
      setContentType('text/plain');
      setBodyText('Hello from Spring Web Captor demo!');
      setTab('body');
    } else if (s.path === '/demo/errors/validation') {
      setContentType('application/json');
      setBodyText(JSON.stringify({ description: 'Missing name field' }, null, 2));
      setTab('body');
    } else {
      setContentType('');
      setBodyText('');
    }
  };

  const tabs: { key: Tab; label: string }[] = [
    { key: 'headers', label: `Headers${headers.filter((h) => h.key).length ? ` (${headers.filter((h) => h.key).length})` : ''}` },
    { key: 'body', label: 'Body' },
  ];

  const tryParseJson = (text: string) => {
    try { return JSON.parse(text); } catch { return null; }
  };

  return (
    <div className="flex flex-col h-full">
      {/* Method + URL */}
      <div className="px-4 py-3 border-b border-slate-800">
        <div className="flex gap-2">
          <select
            value={method}
            onChange={(e) => setMethod(e.target.value)}
            className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm font-bold font-mono text-slate-200 focus:outline-none focus:ring-1 focus:ring-blue-500 cursor-pointer"
          >
            {METHODS.map((m) => (
              <option key={m} value={m}>{m}</option>
            ))}
          </select>
          <div className="relative flex-1">
            <div className="flex items-center bg-slate-800 border border-slate-700 rounded-lg overflow-hidden focus-within:ring-1 focus-within:ring-blue-500">
              <span className="pl-3 text-sm text-slate-500 select-none shrink-0">localhost:8085</span>
              <input
                ref={inputRef}
                type="text"
                value={path}
                onChange={(e) => { setPath(e.target.value); setShowSuggestions(true); }}
                onFocus={() => setShowSuggestions(true)}
                onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
                placeholder="/demo/items"
                className="flex-1 bg-transparent px-1 py-2 text-sm font-mono text-slate-200 placeholder:text-slate-600 focus:outline-none"
              />
            </div>
            {showSuggestions && filteredSuggestions.length > 0 && (
              <div className="absolute top-full left-0 right-0 mt-1 bg-slate-800 border border-slate-700 rounded-lg shadow-xl z-50 max-h-64 overflow-auto">
                {filteredSuggestions.map((s, i) => (
                  <button
                    key={i}
                    onMouseDown={() => selectSuggestion(s)}
                    className="w-full text-left px-3 py-2 hover:bg-slate-700 flex items-center gap-2 text-sm"
                  >
                    <MethodBadge method={s.method} />
                    <span className="font-mono text-slate-300 truncate">{s.path}</span>
                    <span className="text-slate-500 text-xs ml-auto shrink-0">{s.label}</span>
                  </button>
                ))}
              </div>
            )}
          </div>
          <button
            onClick={handleSend}
            disabled={loading}
            className="flex items-center gap-2 bg-blue-600 hover:bg-blue-500 disabled:bg-blue-800 text-white rounded-lg px-4 py-2 text-sm font-medium transition-colors"
          >
            {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
            Send
          </button>
        </div>
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

      {/* Tab content */}
      <div className="flex-1 overflow-auto p-4">
        {tab === 'headers' && (
          <KeyValueEditor pairs={headers} onChange={setHeaders} keyPlaceholder="Header name" valuePlaceholder="Value" />
        )}

        {tab === 'body' && (
          <div className="space-y-3">
            <div className="flex flex-wrap gap-2">
              {CONTENT_TYPES.map((ct) => (
                <button
                  key={ct.value}
                  onClick={() => setContentType(ct.value)}
                  className={`px-3 py-1 rounded-full text-xs font-medium border transition-colors ${
                    contentType === ct.value
                      ? 'bg-blue-500/15 text-blue-400 border-blue-500/30'
                      : 'bg-slate-800 text-slate-400 border-slate-700 hover:border-slate-600'
                  }`}
                >
                  {ct.label}
                </button>
              ))}
            </div>

            {(contentType === 'application/json' || contentType === 'text/plain') && (
              <textarea
                value={bodyText}
                onChange={(e) => setBodyText(e.target.value)}
                placeholder={contentType === 'application/json' ? '{\n  "key": "value"\n}' : 'Plain text body...'}
                rows={8}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm font-mono text-slate-200 placeholder:text-slate-600 focus:outline-none focus:ring-1 focus:ring-blue-500 resize-y"
              />
            )}

            {contentType === 'application/x-www-form-urlencoded' && (
              <KeyValueEditor pairs={formFields} onChange={setFormFields} keyPlaceholder="Field name" valuePlaceholder="Value" />
            )}

            {contentType === 'multipart/form-data' && (
              <div className="space-y-3">
                <KeyValueEditor pairs={formFields} onChange={setFormFields} keyPlaceholder="Field name" valuePlaceholder="Value" />
                <div>
                  <label className="block text-xs text-slate-500 mb-1">File</label>
                  <input
                    type="file"
                    onChange={(e) => setFile(e.target.files?.[0] || null)}
                    className="text-sm text-slate-400 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border file:border-slate-700 file:bg-slate-800 file:text-slate-300 file:text-sm hover:file:bg-slate-700 file:cursor-pointer"
                  />
                  {file && <div className="text-xs text-slate-500 mt-1">{file.name} ({(file.size / 1024).toFixed(1)} KB)</div>}
                </div>
              </div>
            )}

            {contentType === '' && (
              <div className="text-sm text-slate-500 italic">No body for this request</div>
            )}
          </div>
        )}
      </div>

      {/* Response */}
      {response && (
        <div className="border-t border-slate-800 max-h-[40%] overflow-auto">
          <div className="px-4 py-2 bg-slate-900/50 border-b border-slate-800 flex items-center gap-2">
            <span className="text-xs font-medium text-slate-400">Response</span>
            <StatusBadge status={response.status} />
            <span className="text-xs text-slate-500">{response.statusText}</span>
          </div>
          <div className="p-4">
            {tryParseJson(response.body) ? (
              <JsonViewer data={tryParseJson(response.body)} />
            ) : (
              <pre className="text-sm font-mono text-slate-300 whitespace-pre-wrap break-all">{response.body}</pre>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
