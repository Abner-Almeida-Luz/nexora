import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { Plus, Pencil, Trash2 } from 'lucide-react';
import { ApiError } from '../services/api';
import { productsApi } from '../services/productService';
import { formatCurrency } from '../utils/currency';
import type { Product } from '../types';

export default function AdminProducts() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const loadProducts = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await productsApi.list({ page, size: 20 });
      setProducts(response.content);
      setTotalPages(response.totalPages);
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Não foi possível carregar os produtos.');
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => { void loadProducts(); }, [loadProducts]);

  async function handleRemove(product: Product) {
    if (!window.confirm(`Apagar o produto "${product.name}"?`)) return;

    try {
      await productsApi.remove(product.id);
      toast.success('Produto apagado.');
      await loadProducts();
    } catch (err) {
      toast.error(err instanceof ApiError ? err.detail : 'Não foi possível apagar o produto.');
    }
  }

  return (
    <section>
      <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold">Produtos</h1>
          <p className="mt-2 text-zinc-400">Gerencie o catálogo da loja.</p>
        </div>
        <Link to="/admin/products/new" className="inline-flex items-center gap-2 rounded-xl bg-[#8257E5] px-4 py-2 font-medium hover:bg-[#7047D0]"><Plus size={18} /> Novo produto</Link>
      </div>

      {loading && <div className="py-16 text-center text-zinc-400">Carregando produtos...</div>}
      {!loading && error && <div className="rounded-2xl border border-red-500/20 bg-red-500/10 p-6 text-red-300">{error}</div>}
      {!loading && !error && products.length === 0 && <div className="rounded-2xl border border-white/10 bg-[#111113] p-8 text-zinc-400">Nenhum produto cadastrado.</div>}

      {!loading && !error && products.length > 0 && (
        <div className="overflow-hidden rounded-2xl border border-white/10 bg-[#111113]">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-sm">
              <thead className="border-b border-white/10 text-zinc-500">
                <tr><th className="px-5 py-4">Produto</th><th className="px-5 py-4">Categoria</th><th className="px-5 py-4">Preço</th><th className="px-5 py-4">Estoque</th><th className="px-5 py-4 text-right">Ações</th></tr>
              </thead>
              <tbody>
                {products.map((product) => (
                  <tr key={product.id} className="border-b border-white/5 last:border-0">
                    <td className="px-5 py-4 font-medium">{product.name}</td>
                    <td className="px-5 py-4 text-zinc-400">{product.categoryName}</td>
                    <td className="px-5 py-4">{formatCurrency(product.price)}</td>
                    <td className="px-5 py-4 text-zinc-400">{product.stock}</td>
                    <td className="px-5 py-4"><div className="flex justify-end gap-2"><Link to={`/admin/products/${product.id}/edit`} className="rounded-lg p-2 text-zinc-400 hover:bg-white/5 hover:text-white" aria-label={`Editar ${product.name}`}><Pencil size={17} /></Link><button type="button" onClick={() => void handleRemove(product)} className="rounded-lg p-2 text-zinc-400 hover:bg-red-500/10 hover:text-red-400" aria-label={`Apagar ${product.name}`}><Trash2 size={17} /></button></div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {totalPages > 1 && (
        <div className="mt-6 flex items-center justify-center gap-4">
          <button disabled={page === 0} onClick={() => setPage((value) => value - 1)} className="rounded-xl border border-white/10 px-4 py-2 disabled:opacity-40">Anterior</button>
          <span className="text-sm text-zinc-400">Página {page + 1} de {totalPages}</span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage((value) => value + 1)} className="rounded-xl border border-white/10 px-4 py-2 disabled:opacity-40">Próxima</button>
        </div>
      )}
    </section>
  );
}
