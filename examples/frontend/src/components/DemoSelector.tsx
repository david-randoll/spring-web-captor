import type { DemoScenario } from './demoData';
import MethodBadge from './MethodBadge';

interface Props {
  demos: DemoScenario[];
  selected: DemoScenario;
  onSelect: (demo: DemoScenario) => void;
}

export default function DemoSelector({ demos, selected, onSelect }: Props) {
  return (
    <div>
      <p className="text-xs text-slate-500 mb-3">Choose a scenario to see what Spring Web Captor captures:</p>
      <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-thin">
        {demos.map((demo) => (
          <button
            key={demo.id}
            onClick={() => onSelect(demo)}
            className={`shrink-0 flex items-center gap-2 px-3 py-2 rounded-lg text-xs font-medium border transition-all ${
              selected.id === demo.id
                ? 'bg-blue-500/10 border-blue-500/40 text-blue-400'
                : 'bg-slate-900/50 border-slate-800 text-slate-400 hover:border-slate-700 hover:text-slate-300'
            }`}
          >
            <MethodBadge method={demo.method} />
            <span>{demo.title}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
