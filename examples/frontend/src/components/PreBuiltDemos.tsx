import { useState } from 'react';
import { sendRequest, clearCapturedEvents } from '../api/client';
import {
  Database,
  Upload,
  AlertTriangle,
  GitBranch,
  Search,
  Timer,
  Zap,
  Loader2,
  Play,
} from 'lucide-react';

const delay = (ms: number) => new Promise((r) => setTimeout(r, ms));

interface Demo {
  id: string;
  icon: React.ReactNode;
  title: string;
  description: string;
  run: () => Promise<void>;
}

function buildDemos(): Demo[] {
  return [
    {
      id: 'crud',
      icon: <Database className="w-5 h-5" />,
      title: 'JSON CRUD',
      description: 'Create, read, update, patch, and delete — all HTTP methods captured',
      run: async () => {
        await clearCapturedEvents();
        await delay(200);
        await sendRequest('POST', '/demo/items', {
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name: 'Demo Widget', description: 'Created by demo', tags: ['demo'] }),
        });
        await delay(300);
        await sendRequest('GET', '/demo/items');
        await delay(300);
        await sendRequest('GET', '/demo/items/1');
        await delay(300);
        await sendRequest('PUT', '/demo/items/1', {
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name: 'Updated Widget', description: 'Modified', tags: ['updated'] }),
        });
        await delay(300);
        await sendRequest('PATCH', '/demo/items/1', {
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name: 'Patched Widget' }),
        });
        await delay(300);
        await sendRequest('DELETE', '/demo/items/2');
      },
    },
    {
      id: 'upload',
      icon: <Upload className="w-5 h-5" />,
      title: 'File Upload',
      description: 'Multipart upload with file metadata and base64 capture',
      run: async () => {
        await clearCapturedEvents();
        await delay(200);
        const fd = new FormData();
        fd.append('file', new Blob(['Hello from Spring Web Captor!'], { type: 'text/plain' }), 'demo.txt');
        fd.append('description', 'Demo file upload');
        await sendRequest('POST', '/demo/upload', { body: fd });
      },
    },
    {
      id: 'errors',
      icon: <AlertTriangle className="w-5 h-5" />,
      title: 'Error Showcase',
      description: 'See how the library captures 400, 404, 500, and 418 error details',
      run: async () => {
        await clearCapturedEvents();
        await delay(200);
        await sendRequest('GET', '/demo/errors/400').catch(() => {});
        await delay(300);
        await sendRequest('GET', '/demo/errors/404').catch(() => {});
        await delay(300);
        await sendRequest('GET', '/demo/errors/500').catch(() => {});
        await delay(300);
        await sendRequest('GET', '/demo/errors/418').catch(() => {});
        await delay(300);
        await sendRequest('POST', '/demo/errors/validation', {
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ description: 'Missing required name' }),
        }).catch(() => {});
      },
    },
    {
      id: 'paths',
      icon: <GitBranch className="w-5 h-5" />,
      title: 'Path Variables',
      description: 'Named path params and catch-all wildcard path capturing',
      run: async () => {
        await clearCapturedEvents();
        await delay(200);
        await sendRequest('GET', '/demo/paths/electronics/42');
        await delay(300);
        await sendRequest('GET', '/demo/paths/wildcard/segment1/segment2/segment3/deep');
        await delay(300);
        await sendRequest('GET', '/demo/paths/books/978');
      },
    },
    {
      id: 'search',
      icon: <Search className="w-5 h-5" />,
      title: 'Query Parameters',
      description: 'Complex multi-value query parameter capturing',
      run: async () => {
        await clearCapturedEvents();
        await delay(200);
        await sendRequest('GET', '/demo/search?q=spring+web+captor&page=1&size=20&sort=relevance&filters=active&filters=premium&filters=verified');
        await delay(300);
        await sendRequest('GET', '/demo/search?q=&page=1&size=5');
      },
    },
    {
      id: 'slow',
      icon: <Timer className="w-5 h-5" />,
      title: 'Slow Request',
      description: 'Duration extension tracks exactly how long the request took',
      run: async () => {
        await clearCapturedEvents();
        await delay(200);
        await sendRequest('GET', '/demo/slow/2');
      },
    },
    {
      id: 'concurrent',
      icon: <Zap className="w-5 h-5" />,
      title: 'Concurrent Blast',
      description: 'Fire 10 simultaneous requests — all captured thread-safely',
      run: async () => {
        await clearCapturedEvents();
        await delay(200);
        await Promise.all(
          Array.from({ length: 10 }, (_, i) =>
            sendRequest('GET', `/demo/items/${(i % 3) + 1}`)
          )
        );
      },
    },
  ];
}

export default function PreBuiltDemos() {
  const [running, setRunning] = useState<string | null>(null);
  const demos = buildDemos();

  const handleRun = async (demo: Demo) => {
    setRunning(demo.id);
    try {
      await demo.run();
    } catch {
      // ignore
    }
    setRunning(null);
  };

  return (
    <div className="p-4 space-y-3">
      <div className="mb-4">
        <h3 className="text-sm font-semibold text-slate-200 mb-1">Quick Demos</h3>
        <p className="text-xs text-slate-500">Run pre-built scenarios to see the library in action. Watch the Captured Events panel on the right.</p>
      </div>
      <div className="grid gap-3">
        {demos.map((demo) => {
          const isRunning = running === demo.id;
          return (
            <button
              key={demo.id}
              onClick={() => handleRun(demo)}
              disabled={running !== null}
              className="flex items-start gap-3 bg-slate-900 border border-slate-800 rounded-lg p-4 text-left hover:border-slate-700 hover:bg-slate-800/50 disabled:opacity-50 disabled:cursor-not-allowed transition-all group"
            >
              <div className={`p-2 rounded-lg shrink-0 ${isRunning ? 'bg-blue-500/20 text-blue-400' : 'bg-slate-800 text-slate-400 group-hover:text-blue-400 group-hover:bg-blue-500/10'} transition-colors`}>
                {isRunning ? <Loader2 className="w-5 h-5 animate-spin" /> : demo.icon}
              </div>
              <div className="flex-1 min-w-0">
                <div className="font-medium text-sm text-slate-200 mb-0.5">{demo.title}</div>
                <div className="text-xs text-slate-500">{demo.description}</div>
              </div>
              {!isRunning && (
                <Play className="w-4 h-4 text-slate-600 group-hover:text-blue-400 mt-1 shrink-0 transition-colors" />
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}
