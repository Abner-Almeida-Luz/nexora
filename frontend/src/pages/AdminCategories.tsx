import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { toast } from 'react-hot-toast';
import { Pencil, Trash2 } from 'lucide-react';
import { ApiError } from '../services/api';
import { categoriesApi } from '../services/categoryService';
import type { Category } from '../types';

export default function AdminCategories() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [name, setName] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const loadCategories = useCallback(async () => {
    try {
      setLoading(true);
      setCategories(await categoriesApi.list());
    } catch (err) {
      toast.error(err instanceof ApiError ? err.detail : 'Não foi possível carregar categorias.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void loadCategories(); }, [loadCategories]);

  function startEdit(category: Category) {
    setEditingId(category.id);
    setName(category.name);
  }

  function resetForm() {
    setEditingId(null);
    setName('');
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!name.trim()) {
      toast.error('Informe o nome da categoria.');
      return;
    }

    try {
      setSaving(true);
      if (editingId === null) {
        await categoriesApi.create({ name: name.trim() });
        toast.success('Categoria criada.');
      } else {
        await categoriesApi.update(editingId, { name: name.trim() });
        toast.success('Categoria atualizada.');
      }
      resetForm();
      await loadCategories();
    } catch (err) {
      toast.error(err instanceof ApiError ? err.detail : 'Não foi possível salvar a categoria.');
    } finally {
      setSaving(false);
    }
  }

  async function handleRemove(category: Category) {
    if (!window.confirm(`Apagar a categoria "${category.name}"?`)) return;

    try {
      await categoriesApi.remove(category.id);
      toast.success('Categoria apagada.');
      if (editingId === category.id) resetForm();
      await loadCategories();
    } catch (err) {
      toast.error(err instanceof ApiError ? err.detail : 'Não foi possível apagar a categoria.');
    }
  }

  return (
    <section>
      <div className="mb-8"><h1 className="text-3xl font-bold">Categorias</h1><p className="mt-2 text-zinc-400">Crie e organize as categorias do catálogo.</p></div>

      <form onSubmit={handleSubmit} className="mb-8 flex flex-col gap-3 rounded-2xl border border-white/10 bg-[#111113] p-5 sm:flex-row">
        <input value={name} onChange={(event) => setName(event.target.value)} placeholder="Nome da categoria" className="flex-1 rounded-xl border border-white/10 bg-[#09090B] px-4 py-3 outline-none focus:border-[#8257E5]" />
        <button disabled={saving} className="rounded-xl bg-[#8257E5] px-5 py-3 font-medium disabled:opacity-50">{saving ? 'Salvando...' : editingId === null ? 'Criar' : 'Salvar'}</button>
        {editingId !== null && <button type="button" onClick={resetForm} className="rounded-xl border border-white/10 px-5 py-3 text-zinc-300 hover:bg-white/5">Cancelar</button>}
      </form>

      {loading ? <div className="py-12 text-center text-zinc-400">Carregando categorias...</div> : (
        <div className="overflow-hidden rounded-2xl border border-white/10 bg-[#111113]">
          {categories.map((category) => (
            <div key={category.id} className="flex items-center justify-between gap-4 border-b border-white/5 px-5 py-4 last:border-0">
              <span className="font-medium">{category.name}</span>
              <div className="flex gap-2"><button type="button" onClick={() => startEdit(category)} className="rounded-lg p-2 text-zinc-400 hover:bg-white/5 hover:text-white" aria-label={`Editar ${category.name}`}><Pencil size={17} /></button><button type="button" onClick={() => void handleRemove(category)} className="rounded-lg p-2 text-zinc-400 hover:bg-red-500/10 hover:text-red-400" aria-label={`Apagar ${category.name}`}><Trash2 size={17} /></button></div>
            </div>
          ))}
          {categories.length === 0 && <div className="p-8 text-zinc-400">Nenhuma categoria cadastrada.</div>}
        </div>
      )}
    </section>
  );
}
