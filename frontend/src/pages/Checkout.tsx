import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { ApiError } from '../services/api';
import { createOrder } from '../services/orderService';

export default function Checkout() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  async function handleCheckout() {
    try {
      setLoading(true);
      const order = await createOrder();
      toast.success(`Pedido #${order.id} criado com sucesso.`);
      navigate(`/orders/${order.id}`);
    } catch (err) {
      toast.error(err instanceof ApiError ? err.detail : 'Não foi possível finalizar o pedido.');
    } finally {
      setLoading(false);
    }
  }

  return <section className="mx-auto max-w-2xl py-12"><h1 className="text-3xl font-bold">Checkout</h1><p className="mt-3 text-zinc-400">O backend criará o pedido a partir do seu carrinho autenticado.</p><div className="mt-8 rounded-2xl border border-white/10 bg-[#111113] p-6"><p className="text-sm leading-6 text-zinc-400">Ao confirmar, a API valida estoque e cria o pedido. O carrinho será processado pelo backend.</p><button disabled={loading} onClick={() => void handleCheckout()} className="mt-6 w-full rounded-xl bg-[#8257E5] px-5 py-3 font-medium disabled:opacity-50">{loading ? 'Processando...' : 'Confirmar pedido'}</button></div></section>;
}
