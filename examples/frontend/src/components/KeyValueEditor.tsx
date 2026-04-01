import { Plus, X } from 'lucide-react';

interface KeyValuePair {
  key: string;
  value: string;
}

interface Props {
  pairs: KeyValuePair[];
  onChange: (pairs: KeyValuePair[]) => void;
  keyPlaceholder?: string;
  valuePlaceholder?: string;
}

export default function KeyValueEditor({ pairs, onChange, keyPlaceholder = 'Key', valuePlaceholder = 'Value' }: Props) {
  const update = (index: number, field: 'key' | 'value', val: string) => {
    const next = [...pairs];
    next[index] = { ...next[index], [field]: val };
    onChange(next);
  };

  const remove = (index: number) => {
    onChange(pairs.filter((_, i) => i !== index));
  };

  const add = () => {
    onChange([...pairs, { key: '', value: '' }]);
  };

  return (
    <div className="space-y-2">
      {pairs.map((pair, i) => (
        <div key={i} className="flex gap-2">
          <input
            type="text"
            value={pair.key}
            onChange={(e) => update(i, 'key', e.target.value)}
            placeholder={keyPlaceholder}
            className="flex-1 bg-slate-800 border border-slate-700 rounded px-3 py-1.5 text-sm text-slate-200 placeholder:text-slate-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <input
            type="text"
            value={pair.value}
            onChange={(e) => update(i, 'value', e.target.value)}
            placeholder={valuePlaceholder}
            className="flex-1 bg-slate-800 border border-slate-700 rounded px-3 py-1.5 text-sm text-slate-200 placeholder:text-slate-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <button onClick={() => remove(i)} className="p-1.5 text-slate-500 hover:text-red-400 transition-colors">
            <X className="w-4 h-4" />
          </button>
        </div>
      ))}
      <button
        onClick={add}
        className="flex items-center gap-1 text-xs text-slate-400 hover:text-blue-400 transition-colors"
      >
        <Plus className="w-3 h-3" /> Add
      </button>
    </div>
  );
}
