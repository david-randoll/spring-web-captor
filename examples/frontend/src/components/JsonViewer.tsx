import { useState } from 'react';
import { ChevronRight, ChevronDown, Copy, Check } from 'lucide-react';

function StringValue({ value }: { value: string }) {
  const [expanded, setExpanded] = useState(false);
  const truncateAt = 200;

  if (value.length <= truncateAt) {
    return <span className="json-string">"{value}"</span>;
  }

  return (
    <span className="json-string">
      "{expanded ? value : value.slice(0, truncateAt)}
      {!expanded && (
        <button
          onClick={() => setExpanded(true)}
          className="inline ml-1 text-blue-400 hover:text-blue-300 text-xs underline underline-offset-2 cursor-pointer"
        >
          ...show more
        </button>
      )}
      {expanded && (
        <button
          onClick={() => setExpanded(false)}
          className="inline ml-1 text-blue-400 hover:text-blue-300 text-xs underline underline-offset-2 cursor-pointer"
        >
          show less
        </button>
      )}
      "
    </span>
  );
}

function JsonValue({ value, depth = 0 }: { value: unknown; depth?: number }) {
  const [collapsed, setCollapsed] = useState(depth > 2);

  if (value === null || value === undefined) {
    return <span className="json-null">null</span>;
  }

  if (typeof value === 'boolean') {
    return <span className="json-boolean">{String(value)}</span>;
  }

  if (typeof value === 'number') {
    return <span className="json-number">{value}</span>;
  }

  if (typeof value === 'string') {
    return <StringValue value={value} />;
  }

  if (Array.isArray(value)) {
    if (value.length === 0) return <span className="text-slate-400">[]</span>;
    if (collapsed) {
      return (
        <span className="cursor-pointer hover:text-white" onClick={() => setCollapsed(false)}>
          <ChevronRight className="inline w-3 h-3" /> [{value.length} items]
        </span>
      );
    }
    return (
      <span>
        <span className="cursor-pointer hover:text-white" onClick={() => setCollapsed(true)}>
          <ChevronDown className="inline w-3 h-3" /> [
        </span>
        <div className="ml-4">
          {value.map((item, i) => (
            <div key={i}>
              <JsonValue value={item} depth={depth + 1} />
              {i < value.length - 1 && ','}
            </div>
          ))}
        </div>
        ]
      </span>
    );
  }

  if (typeof value === 'object') {
    const entries = Object.entries(value as Record<string, unknown>);
    if (entries.length === 0) return <span className="text-slate-400">{'{}'}</span>;
    if (collapsed) {
      return (
        <span className="cursor-pointer hover:text-white" onClick={() => setCollapsed(false)}>
          <ChevronRight className="inline w-3 h-3" /> {'{'}...{'}'}
        </span>
      );
    }
    return (
      <span>
        <span className="cursor-pointer hover:text-white" onClick={() => setCollapsed(true)}>
          <ChevronDown className="inline w-3 h-3" /> {'{'}
        </span>
        <div className="ml-4">
          {entries.map(([key, val], i) => (
            <div key={key}>
              <span className="json-key">"{key}"</span>: <JsonValue value={val} depth={depth + 1} />
              {i < entries.length - 1 && ','}
            </div>
          ))}
        </div>
        {'}'}
      </span>
    );
  }

  return <span>{String(value)}</span>;
}

export default function JsonViewer({ data, label }: { data: unknown; label?: string }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(JSON.stringify(data, null, 2));
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };

  return (
    <div className="relative">
      {label && <div className="text-xs text-slate-500 mb-1 font-medium uppercase tracking-wide">{label}</div>}
      <div className="bg-slate-950 rounded-lg p-3 font-mono text-sm overflow-auto max-h-96 border border-slate-800">
        <button
          onClick={handleCopy}
          className="absolute top-2 right-2 p-1.5 rounded bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition-colors"
          title="Copy JSON"
        >
          {copied ? <Check className="w-3.5 h-3.5 text-green-400" /> : <Copy className="w-3.5 h-3.5" />}
        </button>
        <JsonValue value={data} />
      </div>
    </div>
  );
}
