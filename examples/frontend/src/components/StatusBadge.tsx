export default function StatusBadge({ status }: { status: string | number | undefined }) {
  if (!status) return null;

  const code = typeof status === 'string' ? parseInt(status) || 0 : status;
  const label = typeof status === 'string' && isNaN(parseInt(status)) ? status : String(code);

  let colors = 'bg-slate-500/15 text-slate-400 border-slate-500/30';
  if (code >= 200 && code < 300) colors = 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30';
  else if (code >= 300 && code < 400) colors = 'bg-yellow-500/15 text-yellow-400 border-yellow-500/30';
  else if (code >= 400 && code < 500) colors = 'bg-orange-500/15 text-orange-400 border-orange-500/30';
  else if (code >= 500) colors = 'bg-red-500/15 text-red-400 border-red-500/30';

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-bold font-mono border ${colors}`}>
      {label}
    </span>
  );
}
