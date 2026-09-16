import { Search } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { useAuth } from '../contexts/useAuth';
import { ApiError } from '../services/api';
import { addCartItem } from '../services/cartService';
import { getCategories } from '../services/categoryService';
import { productsApi } from '../services/productService';
import { ProductCard } from '../components/products/ProductCard';
import { SectionTitle } from '../components/ui/SectionTitle';
import type { Category, Product } from '../types';

export default function Products() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [search, setSearch] = useState('');
  const [categoryId, setCategoryId] = useState<number | undefined>();
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [addingId, setAddingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadProducts = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await productsApi.list({ search, categoryId, page, size: 12 });
      setProducts(response.content);
      setTotalPages(response.totalPages);
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Não foi possível carregar os produtos.');
    } finally {
      setLoading(false);
    }
  }, [search, categoryId, page]);

  useEffect(() => {
    void loadProducts();
  }, [loadProducts]);

  const loadCategories = useCallback(async () => {
    try {
      setCategories(await getCategories());
    } catch {
      // A falha das categorias não impede o carregamento do catálogo.
    }
  }, []);

  useEffect(() => {
    void loadCategories();
  }, [loadCategories]);

  async function handleAddToCart(productId: number) {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: '/products' } });
      return;
    }

    try {
      setAddingId(productId);
      await addCartItem({ productId, quantity: 1 });
      toast.success('Produto adicionado ao carrinho.');
    } catch (err) {
      toast.error(err instanceof ApiError ? err.detail : 'Não foi possível adicionar o produto.');
    } finally {
      setAddingId(null);
    }
  }

  return (
    <section>
      <SectionTitle title="Produtos" description="Explore o catálogo e encontre o que procura." />

      <div className="mb-8 grid gap-3 md:grid-cols-[1fr_240px]">
        <label className="relative block">
          <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
          <input value={search} onChange={(event) => { setPage(0); setSearch(event.target.value); }} placeholder="Buscar produtos..." className="w-full rounded-xl border border-white/10 bg-[#111113] py-3 pl-10 pr-4 outline-none focus:border-[#8257E5]" />
        </label>
        <select value={categoryId ?? ''} onChange={(event) => { setPage(0); setCategoryId(event.target.value ? Number(event.target.value) : undefined); }} className="rounded-xl border border-white/10 bg-[#111113] px-4 py-3 outline-none focus:border-[#8257E5]">
          <option value="">Todas as categorias</option>
          {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
        </select>
      </div>

      {loading && <div className="py-20 text-center text-zinc-400">Carregando produtos...</div>}
      {!loading && error && <div className="rounded-2xl border border-red-500/20 bg-red-500/10 p-6 text-red-300">{error}</div>}
      {!loading && !error && products.length === 0 && <div className="rounded-2xl border border-white/10 bg-[#111113] p-10 text-center text-zinc-400">Nenhum produto encontrado.</div>}

      {!loading && !error && products.length > 0 && (
        <>
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {products.map((product) => <ProductCard key={product.id} product={product} onAddToCart={handleAddToCart} adding={addingId === product.id} />)}
          </div>
          {totalPages > 1 && (
            <div className="mt-8 flex items-center justify-center gap-4">
              <button disabled={page === 0} onClick={() => setPage((value) => value - 1)} className="rounded-xl border border-white/10 px-4 py-2 disabled:opacity-40">Anterior</button>
              <span className="text-sm text-zinc-400">Página {page + 1} de {totalPages}</span>
              <button disabled={page >= totalPages - 1} onClick={() => setPage((value) => value + 1)} className="rounded-xl border border-white/10 px-4 py-2 disabled:opacity-40">Próxima</button>
            </div>
          )}
        </>
      )}
    </section>
  );
}
