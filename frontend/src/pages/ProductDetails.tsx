import { Star, ShoppingCart } from 'lucide-react';
import { useEffect, useState, useCallback, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { useAuth } from '../contexts/useAuth';
import { ApiError } from '../services/api';
import { addCartItem } from '../services/cartService';
import { productsApi } from '../services/productService';
import { createReview, getProductReviews } from '../services/reviewService';
import { formatCurrency } from '../utils/currency';
import type { Product, Review } from '../types';

export default function ProductDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const productId = Number(id);
  const [product, setProduct] = useState<Product | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);
  const [reviewLoading, setReviewLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [reviewError, setReviewError] = useState<string | null>(null);

  const loadProduct = useCallback(async () => {
    if (!Number.isInteger(productId)) {
      setError('Produto não encontrado.');
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      const [nextProduct, reviewPage] = await Promise.all([
        productsApi.getById(productId),
        getProductReviews(productId, { page: 0, size: 10 }),
      ]);
      setProduct(nextProduct);
      setReviews(reviewPage.content);
      setQuantity(1);
      setError(null);
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Produto não encontrado.');
    } finally {
      setLoading(false);
    }
  }, [productId]);

  useEffect(() => { void loadProduct(); }, [loadProduct]);

  async function handleAddToCart() {
    if (!product) return;
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/products/${product.id}` } });
      return;
    }
    try {
      await addCartItem({ productId: product.id, quantity });
      toast.success('Produto adicionado ao carrinho.');
    } catch (err) {
      toast.error(err instanceof ApiError ? err.detail : 'Não foi possível adicionar o produto.');
    }
  }

  async function handleReviewSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setReviewError(null);
    try {
      setReviewLoading(true);
      const review = await createReview({ productId, rating, comment });
      setReviews((current) => [review, ...current]);
      setComment('');
      setRating(5);
      toast.success('Avaliação enviada.');
    } catch (err) {
      setReviewError(err instanceof ApiError ? err.detail : 'Não foi possível enviar a avaliação.');
    } finally {
      setReviewLoading(false);
    }
  }

  if (loading) return <div className="py-20 text-center text-zinc-400">Carregando produto...</div>;
  if (error || !product) return <div className="rounded-2xl border border-white/10 bg-[#111113] p-10 text-center text-zinc-400">{error ?? 'Produto não encontrado.'}</div>;

  return (
    <section>
      <Link to="/products" className="mb-8 inline-block text-sm text-zinc-400 hover:text-white">← Voltar para produtos</Link>
      <div className="grid gap-10 lg:grid-cols-2">
        <div className="overflow-hidden rounded-3xl border border-white/10 bg-[#111113]">{product.imageUrl ? <img src={product.imageUrl} alt={product.name} className="aspect-square w-full object-cover" /> : <div className="flex aspect-square items-center justify-center text-zinc-600">Sem imagem</div>}</div>
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-[#9A6CFF]">{product.categoryName}</p>
          <h1 className="mt-3 text-4xl font-bold">{product.name}</h1>
          <p className="mt-6 text-lg leading-8 text-zinc-400">{product.description ?? 'Sem descrição.'}</p>
          <p className="mt-8 text-3xl font-bold">{formatCurrency(product.price)}</p>
          <p className="mt-2 text-sm text-zinc-500">{product.stock > 0 ? `${product.stock} unidades disponíveis` : 'Produto esgotado'}</p>
          <div className="mt-8 flex max-w-sm gap-3">
            <input type="number" min={1} max={Math.max(product.stock, 1)} value={quantity} onChange={(event) => setQuantity(Math.min(Math.max(1, Number(event.target.value)), Math.max(product.stock, 1)))} className="w-24 rounded-xl border border-white/10 bg-[#111113] px-4 py-3" />
            <button onClick={() => void handleAddToCart()} disabled={product.stock <= 0} className="inline-flex flex-1 items-center justify-center gap-2 rounded-xl bg-[#8257E5] px-5 py-3 font-medium hover:bg-[#7047D0] disabled:opacity-50"><ShoppingCart size={18} /> Adicionar</button>
          </div>
        </div>
      </div>

      <section className="mt-16">
        <div className="mb-8 flex items-center justify-between"><h2 className="text-2xl font-bold">Avaliações</h2><span className="text-sm text-zinc-500">{reviews.length} exibidas</span></div>
        <div className="grid gap-6 lg:grid-cols-[1fr_380px]">
          <div className="space-y-4">{reviews.length === 0 ? <div className="rounded-2xl border border-white/10 bg-[#111113] p-6 text-zinc-400">Ainda não há avaliações.</div> : reviews.map((review) => <article key={review.id} className="rounded-2xl border border-white/10 bg-[#111113] p-5"><div className="flex items-center justify-between gap-4"><strong>{review.userName}</strong><span className="flex items-center gap-1 text-[#B99AFF]">{review.rating} <Star size={14} fill="currentColor" /></span></div><p className="mt-3 text-sm leading-6 text-zinc-400">{review.comment ?? 'Sem comentário.'}</p><time className="mt-3 block text-xs text-zinc-600">{new Date(review.createdAt).toLocaleDateString('pt-BR')}</time></article>)}</div>
          {isAuthenticated && <form onSubmit={handleReviewSubmit} className="h-fit rounded-2xl border border-white/10 bg-[#111113] p-6"><h3 className="text-lg font-semibold">Avaliar produto</h3>{reviewError && <div className="mt-4 rounded-xl border border-red-500/20 bg-red-500/10 p-3 text-sm text-red-300">{reviewError}</div>}<label className="mt-5 block text-sm text-zinc-300">Nota</label><select value={rating} onChange={(event) => setRating(Number(event.target.value))} className="mt-2 w-full rounded-xl border border-white/10 bg-[#09090B] px-4 py-3"><option value="5">5 - Excelente</option><option value="4">4 - Muito bom</option><option value="3">3 - Bom</option><option value="2">2 - Ruim</option><option value="1">1 - Péssimo</option></select><label className="mt-5 block text-sm text-zinc-300">Comentário</label><textarea required minLength={3} value={comment} onChange={(event) => setComment(event.target.value)} rows={5} className="mt-2 w-full rounded-xl border border-white/10 bg-[#09090B] px-4 py-3 outline-none focus:border-[#8257E5]" /><button disabled={reviewLoading} className="mt-5 w-full rounded-xl bg-[#8257E5] px-5 py-3 font-medium disabled:opacity-50">{reviewLoading ? 'Enviando...' : 'Enviar avaliação'}</button></form>}
        </div>
      </section>
    </section>
  );
}
