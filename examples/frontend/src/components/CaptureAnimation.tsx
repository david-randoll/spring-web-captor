import { useEffect, useState } from 'react';
import type { DemoScenario } from './demoData';
import MethodBadge from './MethodBadge';
import { Radio } from 'lucide-react';

interface Props {
  demo: DemoScenario;
}

const STEPS = [
  'Sending HTTP request...',
  'Request intercepted by Spring Web Captor',
  'Capturing headers, body, params...',
  'Processing response...',
  'Capturing response body & status...',
  'Publishing HttpResponseEvent...',
];

export default function CaptureAnimation({ demo }: Props) {
  const [step, setStep] = useState(0);

  useEffect(() => {
    if (step < STEPS.length - 1) {
      const timer = setTimeout(() => setStep((s) => s + 1), 450);
      return () => clearTimeout(timer);
    }
  }, [step]);

  const shortUrl = demo.url.split('?')[0];
  // Truncate long paths on mobile
  const displayUrl = shortUrl.length > 30 ? shortUrl.slice(0, 30) + '...' : shortUrl;

  return (
    <div className="max-w-2xl mx-auto text-center">
      <div className="relative py-10 sm:py-16">
        {/* Request badge floating */}
        <div className="flex justify-center mb-8 sm:mb-10 animate-bounce-slow">
          <div className="flex items-center gap-2 bg-slate-900 border border-slate-700 rounded-xl px-3 sm:px-5 py-2.5 sm:py-3 shadow-lg shadow-blue-500/5 max-w-full">
            <MethodBadge method={demo.method} />
            <span className="font-mono text-xs sm:text-sm text-slate-300 truncate">{displayUrl}</span>
          </div>
        </div>

        {/* Capture pulse */}
        <div className="flex justify-center mb-8 sm:mb-10">
          <div className="relative">
            <div className="w-14 h-14 sm:w-16 sm:h-16 rounded-full bg-blue-500/10 border-2 border-blue-500/30 flex items-center justify-center animate-pulse">
              <Radio className="w-6 h-6 sm:w-7 sm:h-7 text-blue-400" />
            </div>
            <div className="absolute inset-0 w-14 h-14 sm:w-16 sm:h-16 rounded-full border-2 border-blue-400/20 animate-ping" />
          </div>
        </div>

        {/* Steps */}
        <div className="space-y-2 px-2">
          {STEPS.map((label, i) => (
            <div
              key={label}
              className={`text-xs sm:text-sm font-mono transition-all duration-300 ${
                i < step
                  ? 'text-slate-600'
                  : i === step
                    ? 'text-blue-400 font-medium'
                    : 'text-slate-800'
              }`}
            >
              {i <= step && (
                <span className={`inline-block w-4 mr-1 sm:mr-2 ${i < step ? 'text-emerald-500' : 'text-blue-400'}`}>
                  {i < step ? '✓' : '›'}
                </span>
              )}
              {i <= step ? label : ''}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
