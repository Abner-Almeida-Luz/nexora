import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { ApiError } from '../services/api';
import { ordersApi } from '../services/orderService';
import { formatCurrency } from '../utils/currency';
import type { Order } from '../types';

export default function Orders() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [cancellingId, setCancellingId] = useState<number | null>(null);

  const loadOrders = useCallback(async () => {
    try {
      setLoading(true);
      setOrders(await ordersApi.list());
    } catch (err) {
      toast.error(err instanceof ApiError ? err.detail : 'Não foi possível carregar seus pedidos.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadOrders();
  }, [loadOrders]);

  async function handleCancel(id: number) {
    if (!window.confirm(`Cancelar o pedido #${id}?`)) return;

    try {
      setCancellingId(id);
      const updated = await ordersApi.cancel(id);
      setOrders((current) => current.map((order) => order.id === id ? updated : order));
      toast.success('Pedido cancelado.');
    } catch (err) {
      toast.error(err instanceof ApiError ? err.detail : 'Não foi possível cancelar o pedido.');
    } finally {
      setCancellingId(null);
    }
  }

  if (loading) return <div className="py-20 text-center text-zinc-400">Carregando pedidos...</div>;

  return <section><h1 className="text-3xl font-bold">Meus pedidos</h1><p className="mt-2 text-zinc-400">Histórico de pedidos da sua conta.</p>{orders.length === 0 ? <div className="mt-8 rounded-2xl border border-white/10 bg-[#111113] p-8 text-zinc-400">Você ainda não possui pedidos.</div> : <div className="mt-8 space-y-4">{orders.map((order) => <article key={order.id} className="rounded-2xl border border-white/10 bg-[#111113] p-5 hover:border-[#8257E5]/40"><Link to={`/orders/${order.id}`} className="block"><div className="flex items-center justify-between gap-4"><strong>Pedido #{order.id}</strong><span className="text-sm text-[#B99AFF]">{order.status}</span></div><p className="mt-2 text-sm text-zinc-500">{new Date(order.createdAt).toLocaleString('pt-BR')}</p><div className="mt-4 flex justify-between"><span className="text-sm text-zinc-400">{order.items.length} item(ns)</span><strong>{formatCurrency(order.total)}</strong></div></Link>{order.status === 'PENDING' && <button type="button" disabled={cancellingId === order.id} onClick={() => void handleCancel(order.id)} className="mt-4 rounded-xl border border-red-500/20 px-4 py-2 text-sm text-red-300 hover:bg-red-500/10 disabled:opacity-50">{cancellingId === order.id ? 'Cancelando...' : 'Cancelar pedido'}</button>}</article>)}</div>}</section>;
}
