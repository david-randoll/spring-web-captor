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
  FileText,
  Loader2,
  Play,
} from 'lucide-react';

const delay = (ms: number) => new Promise((r) => setTimeout(r, ms));

interface Demo {
  id: string;
  icon: React.ReactNode;
  title: string;
  description: string;
  tags: string[];
  run: () => Promise<void>;
}

function buildDemos(): Demo[] {
  return [
    {
      id: 'crud',
      icon: <Database className="w-5 h-5" />,
      title: 'JSON CRUD Operations',
      description: 'Full lifecycle: create, list, get, update, patch, and delete. See how every HTTP method and JSON body is captured.',
      tags: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'JSON'],
      run: async () => {
        await clearCapturedEvents();
        await delay(200);
        await sendRequest('POST', '/demo/items', {
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name: 'Demo Widget', description: 'Created by demo', tags: ['demo'] }),
        });
        await delay(400);
        await sendRequest('GET', '/demo/items');
        await delay(400);
        await sendRequest('GET', '/demo/items/1');
        await delay(400);
        await sendRequest('PUT', '/demo/items/1', {
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name: 'Updated Widget', description: 'Modified by demo', tags: ['updated'] }),
        });
        await delay(400);
        await sendRequest('PATCH', '/demo/items/1', {
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name: 'Patched Widget' }),
        });
        await delay(400);
        await sendRequest('DELETE', '/demo/items/2');
      },
    },
    {
      id: 'upload',
      icon: <Upload className="w-5 h-5" />,
      title: 'File Upload',
      description: 'Multipart upload with file metadata captured — filename, size, content type, and base64 content.',
      tags: ['POST', 'Multipart', 'Files'],
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
      id: 'form',
      icon: <FileText className="w-5 h-5" />,
      title: 'Form & Text Bodies',
      description: 'URL-encoded form data and plain text bodies — both parsed and captured.',
      tags: ['POST', 'Form', 'Text'],
      run: async () => {
        await clearCapturedEvents();
        await delay(200);
        const params = new URLSearchParams();
        params.append('username', 'john_doe');
        params.append('email', 'john@example.com');
        params.append('role', 'admin');
        await sendRequest('POST', '/demo/form', {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: params.toString(),
        });
        await delay(400);
        await sendRequest('POST', '/demo/text', {
          headers: { 'Content-Type': 'text/plain' },
          body: 'Hello from Spring Web Captor demo!',
        });
      },
    },
    {
      id: 'paths',
      icon: <GitBranch className="w-5 h-5" />,
      title: 'Path Variables',
      description: 'Named path params and catch-all wildcard {*rest} — each segment extracted automatically.',
      tags: ['GET', 'Path Params', 'Wildcard'],
      run: async () => {
        await clearCapturedEvents();
        await delay(200);
        await sendRequest('GET', '/demo/paths/electronics/42');
        await delay(400);
        await sendRequest('GET', '/demo/paths/wildcard/segment1/segment2/segment3/deep');
      },
    },
    {
      id: 'search',
      icon: <Search className="w-5 h-5" />,
      title: 'Query Parameters',
      description: 'Complex multi-value query params — filters, pagination, sorting all captured.',
      tags: ['GET', 'Query Params', 'Multi-value'],
      run: async () => {
        await clearCapturedEvents();
        await delay(200);
        await sendRequest('GET', '/demo/search?q=spring+web+captor&page=1&size=20&sort=relevance&filters=active&filters=premium&filters=verified');
      },
    },
    {
      id: 'errors',
      icon: <AlertTriangle className="w-5 h-5" />,
      title: 'Error Responses',
      description: 'Error status codes with full error detail capture — message, status, and trace.',
      tags: ['400', '404', '500', '418', 'Validation'],
      run: async () => {
        await clearCapturedEvents();
        await delay(200);
        await sendRequest('GET', '/demo/errors/400').catch(() => {});
        await delay(400);
        await sendRequest('GET', '/demo/errors/404').catch(() => {});
        await delay(400);
        await sendRequest('GET', '/demo/errors/500').catch(() => {});
        await delay(400);
        await sendRequest('GET', '/demo/errors/418').catch(() => {});
        await delay(400);
        await sendRequest('POST', '/demo/errors/validation', {
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ description: 'Missing required name' }),
        }).catch(() => {});
      },
    },
    {
      id: 'slow',
      icon: <Timer className="w-5 h-5" />,
      title: 'Duration Tracking',
      description: 'See the duration extension in action — precise timing of slow requests.',
      tags: ['GET', 'Duration', 'Extensions'],
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
      description: '10 simultaneous requests — all captured thread-safely without data mixing.',
      tags: ['GET', 'Concurrency', 'Thread-safe'],
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

export default function DemoScenarios({ onRun }: { onRun: () => void }) {
  const [running, setRunning] = useState<string | null>(null);
  const demos = buildDemos();

  const handleRun = async (demo: Demo) => {
    setRunning(demo.id);
    onRun();
    try {
      await demo.run();
    } catch {
      // ignore
    }
    setRunning(null);
  };

  return (
    <div className="p-4">
      <div className="mb-5">
        <h2 className="text-sm font-semibold text-slate-200 mb-1">Demo Scenarios</h2>
        <p className="text-xs text-slate-500 leading-relaxed">
          Click a scenario to fire requests. The captured events appear on the right — click any event to inspect everything the library captured.
        </p>
      </div>
      <div className="space-y-2">
        {demos.map((demo) => {
          const isRunning = running === demo.id;
          return (
            <button
              key={demo.id}
              onClick={() => handleRun(demo)}
              disabled={running !== null}
              className="w-full text-left bg-slate-900/60 border border-slate-800 rounded-xl p-3.5 hover:border-slate-700 hover:bg-slate-800/50 disabled:opacity-40 disabled:cursor-not-allowed transition-all group"
            >
              <div className="flex items-start gap-3">
                <div className={`p-2 rounded-lg shrink-0 transition-colors ${isRunning ? 'bg-blue-500/20 text-blue-400' : 'bg-slate-800 text-slate-400 group-hover:text-blue-400 group-hover:bg-blue-500/10'}`}>
                  {isRunning ? <Loader2 className="w-5 h-5 animate-spin" /> : demo.icon}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-1">
                    <span className="font-medium text-sm text-slate-200">{demo.title}</span>
                    {!isRunning && <Play className="w-3.5 h-3.5 text-slate-600 group-hover:text-blue-400 shrink-0 transition-colors" />}
                  </div>
                  <p className="text-xs text-slate-500 leading-relaxed mb-2">{demo.description}</p>
                  <div className="flex flex-wrap gap-1">
                    {demo.tags.map((tag) => (
                      <span key={tag} className="text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-500 font-medium">
                        {tag}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}
