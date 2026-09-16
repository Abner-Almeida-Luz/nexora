import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { ApiError } from '../services/api';
import { ordersApi } from '../services/orderService';
import { formatCurrency } from '../utils/currency';
import type { Order } from '../types';

export default function OrderDetails() {
  const { id } = useParams();
  const orderId = Number(id);
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [cancelling, setCancelling] = useState(false);

  const loadOrder = useCallback(async () => {
    try {
      setLoading(true);
      if (!Number.isInteger(orderId)) throw new Error('ID inválido.');
      setOrder(await ordersApi.getById(orderId));
    } catch (err) {
      toast.error(err instanceof ApiError ? err.detail : 'Pedido não encontrado.');
    } finally {
      setLoading(false);
    }
  }, [orderId]);

  useEffect(() => { void loadOrder(); }, [loadOrder]);

  async function handleCancel() {
    if (!order || order.status !== 'PENDING') return;
    if (!window.confirm(`Cancelar o pedido #${order.id}?`)) return;

    try {
      setCancelling(true);
      setOrder(await ordersApi.cancel(order.id));
      toast.success('Pedido cancelado.');
    } catch (err) {
      toast.error(err instanceof ApiError ? err.detail : 'Não foi possível cancelar o pedido.');
    } finally {
      setCancelling(false);
    }
  }

  if (loading) return <div className="py-20 text-center text-zinc-400">Carregando pedido...</div>;
  if (!order) return <div className="rounded-2xl border border-white/10 bg-[#111113] p-10 text-center">Pedido não encontrado.</div>;

  return <section className="mx-auto max-w-3xl"><Link to="/orders" className="text-sm text-zinc-400 hover:text-white">← Meus pedidos</Link><div className="mt-6 rounded-2xl border border-white/10 bg-[#111113] p-6"><div className="flex flex-wrap items-center justify-between gap-4"><div><h1 className="text-2xl font-bold">Pedido #{order.id}</h1><p className="mt-2 text-sm text-zinc-500">{new Date(order.createdAt).toLocaleString('pt-BR')}</p></div><span className="text-[#B99AFF]">{order.status}</span></div><div className="mt-6 space-y-3">{order.items.map((item) => <div key={`${item.productId}-${item.price}`} className="flex justify-between gap-4 border-b border-white/10 py-3"><span>{item.productName} × {item.quantity}</span><span>{formatCurrency(item.subtotal)}</span></div>)}</div><div className="mt-6 flex items-center justify-between text-lg font-semibold"><span>Total</span><span>{formatCurrency(order.total)}</span></div>{order.status === 'PENDING' && <button type="button" disabled={cancelling} onClick={() => void handleCancel()} className="mt-6 rounded-xl border border-red-500/20 px-5 py-3 text-sm text-red-300 hover:bg-red-500/10 disabled:opacity-50">{cancelling ? 'Cancelando...' : 'Cancelar pedido'}</button>}</div></section>;
}
