import { useCallback, useEffect, useState } from 'react';
import { toast } from 'react-hot-toast';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ProductForm, type ProductFormValues } from '../components/products/ProductForm';
import { ApiError } from '../services/api';
import { productsApi } from '../services/productService';
import type { Product } from '../types';

export default function AdminProductEdit() {
  const { id } = useParams();
  const navigate = useNavigate();
  const productId = Number(id);
  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadProduct = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      if (!Number.isInteger(productId)) throw new Error('ID inválido.');
      setProduct(await productsApi.getById(productId));
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Não foi possível carregar o produto.');
    } finally {
      setLoading(false);
    }
  }, [productId]);

  useEffect(() => { void loadProduct(); }, [loadProduct]);

  async function handleSubmit(data: ProductFormValues) {
    await productsApi.update(productId, data);
    toast.success('Produto atualizado.');
    navigate('/admin/products');
  }

  if (loading) return <div className="py-20 text-center text-zinc-400">Carregando produto...</div>;
  if (error || !product) return <div className="rounded-2xl border border-white/10 bg-[#111113] p-8 text-zinc-400">{error ?? 'Produto não encontrado.'}</div>;

  return (
    <section className="mx-auto max-w-2xl">
      <Link to="/admin/products" className="text-sm text-zinc-400 hover:text-white">← Produtos</Link>
      <h1 className="mt-5 text-3xl font-bold">Editar produto</h1>
      <p className="mt-2 text-zinc-400">Atualize as informações do produto.</p>
      <ProductForm
        submitLabel="Salvar alterações"
        initialValues={product}
        onSubmit={handleSubmit}
      />
    </section>
  );
}
