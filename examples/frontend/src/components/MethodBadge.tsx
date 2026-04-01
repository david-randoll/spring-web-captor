const METHOD_COLORS: Record<string, string> = {
  GET: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30',
  POST: 'bg-blue-500/15 text-blue-400 border-blue-500/30',
  PUT: 'bg-amber-500/15 text-amber-400 border-amber-500/30',
  PATCH: 'bg-purple-500/15 text-purple-400 border-purple-500/30',
  DELETE: 'bg-red-500/15 text-red-400 border-red-500/30',
  OPTIONS: 'bg-slate-500/15 text-slate-400 border-slate-500/30',
  HEAD: 'bg-slate-500/15 text-slate-400 border-slate-500/30',
};

export default function MethodBadge({ method }: { method: string }) {
  const colors = METHOD_COLORS[method?.toUpperCase()] || METHOD_COLORS.GET;
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-bold font-mono border ${colors}`}>
      {method}
    </span>
  );
}
