import { Minus, Plus, Trash2 } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { ApiError } from '../services/api';
import { cartApi } from '../services/cartService';
import { formatCurrency } from '../utils/currency';
import type { Cart } from '../types';

export default function Cart() {
  const navigate = useNavigate();
  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<number | null>(null);

  const loadCart = useCallback(async () => {
    try {
      setLoading(true);
      setCart(await cartApi.get());
    } catch (err) {
      toast.error(err instanceof ApiError ? err.detail : 'Não foi possível carregar o carrinho.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadCart();
  }, [loadCart]);

  async function changeQuantity(productId: number, quantity: number) {
    try {
      setUpdatingId(productId);
      setCart(await cartApi.updateItem(productId, quantity));
    } catch (err) {
      toast.error(err instanceof ApiError ? err.detail : 'Não foi possível atualizar o carrinho.');
    } finally {
      setUpdatingId(null);
    }
  }

  async function handleClear() {
    try {
      await cartApi.clear();
      setCart((current) => current ? { ...current, items: [], total: 0 } : current);
      toast.success('Carrinho limpo.');
    } catch (err) {
      toast.error(err instanceof ApiError ? err.detail : 'Não foi possível limpar o carrinho.');
    }
  }

  if (loading) return <div className="py-20 text-center text-zinc-400">Carregando carrinho...</div>;

  if (!cart || cart.items.length === 0) {
    return <section className="mx-auto max-w-2xl py-20 text-center"><h1 className="text-3xl font-bold">Seu carrinho está vazio</h1><p className="mt-3 text-zinc-400">Adicione produtos para continuar.</p><Link to="/products" className="mt-8 inline-block rounded-xl bg-[#8257E5] px-5 py-3 font-medium">Ver produtos</Link></section>;
  }

  return (
    <section>
      <div className="mb-8 flex items-end justify-between gap-4"><div><h1 className="text-3xl font-bold">Seu carrinho</h1><p className="mt-2 text-zinc-400">Revise os itens antes de finalizar.</p></div><button onClick={() => void handleClear()} className="text-sm text-zinc-500 hover:text-red-400">Limpar carrinho</button></div>
      <div className="grid gap-8 lg:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          {cart.items.map((item) => (
            <article key={item.productId} className="flex gap-4 rounded-2xl border border-white/10 bg-[#111113] p-5">
              <div className="min-w-0 flex-1"><h2 className="font-semibold">{item.productName}</h2><p className="mt-1 text-sm text-zinc-500">{formatCurrency(item.unitPrice)} cada</p><div className="mt-4 flex items-center gap-3"><button disabled={updatingId === item.productId} onClick={() => void changeQuantity(item.productId, item.quantity - 1)} className="rounded-lg border border-white/10 p-2"><Minus size={14}/></button><span className="w-6 text-center">{item.quantity}</span><button disabled={updatingId === item.productId} onClick={() => void changeQuantity(item.productId, item.quantity + 1)} className="rounded-lg border border-white/10 p-2"><Plus size={14}/></button></div></div>
              <div className="flex flex-col items-end justify-between"><strong>{formatCurrency(item.subtotal)}</strong><button disabled={updatingId === item.productId} onClick={() => void changeQuantity(item.productId, 0)} className="rounded-lg p-2 text-zinc-500 hover:bg-red-500/10 hover:text-red-400"><Trash2 size={17}/></button></div>
            </article>
          ))}
        </div>
        <aside className="h-fit rounded-2xl border border-white/10 bg-[#111113] p-6"><h2 className="text-lg font-semibold">Resumo</h2><div className="mt-5 flex justify-between text-zinc-400"><span>Total</span><strong className="text-white">{formatCurrency(cart.total)}</strong></div><button onClick={() => navigate('/checkout')} className="mt-6 w-full rounded-xl bg-[#8257E5] px-5 py-3 font-medium hover:bg-[#7047D0]">Ir para checkout</button></aside>
      </div>
    </section>
  );
}
