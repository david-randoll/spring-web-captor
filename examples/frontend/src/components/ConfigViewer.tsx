import { useState, useEffect } from 'react';
import { fetchConfig } from '../api/client';
import JsonViewer from './JsonViewer';
import { Settings, Loader2 } from 'lucide-react';

export default function ConfigViewer() {
  const [config, setConfig] = useState<unknown>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchConfig()
      .then(setConfig)
      .catch(() => setConfig(null))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="p-4">
      <div className="flex items-center gap-2 mb-4">
        <Settings className="w-4 h-4 text-slate-400" />
        <h3 className="text-sm font-semibold text-slate-200">Web Captor Configuration</h3>
      </div>
      <p className="text-xs text-slate-500 mb-4">
        This is the current <code className="bg-slate-800 px-1 py-0.5 rounded text-slate-400">web-captor</code> configuration
        loaded from <code className="bg-slate-800 px-1 py-0.5 rounded text-slate-400">application.yml</code>.
      </p>
      {loading ? (
        <div className="flex items-center justify-center py-8">
          <Loader2 className="w-5 h-5 animate-spin text-slate-500" />
        </div>
      ) : config ? (
        <JsonViewer data={config} />
      ) : (
        <div className="text-sm text-slate-500 italic">Could not load configuration. Is the backend running?</div>
      )}
    </div>
  );
}
