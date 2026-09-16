import { useCallback, useEffect, useState } from 'react';
import { toast } from 'react-hot-toast';
import { ApiError } from '../services/api';
import { ordersApi } from '../services/orderService';
import { formatCurrency } from '../utils/currency';
import type { Order, OrderStatus } from '../types';

const statuses: OrderStatus[] = ['PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

export default function AdminOrders() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<number | null>(null);

  const loadOrders = useCallback(async () => {
    try {
      setLoading(true);
      setOrders(await ordersApi.listAll());
    } catch (err) {
      toast.error(err instanceof ApiError ? err.detail : 'Não foi possível carregar os pedidos.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void loadOrders(); }, [loadOrders]);

  async function handleStatusChange(id: number, status: OrderStatus) {
    try {
      setUpdatingId(id);
      const updated = await ordersApi.updateStatus(id, status);
      setOrders((current) => current.map((order) => order.id === id ? updated : order));
      toast.success(`Pedido #${id} atualizado.`);
    } catch (err) {
      toast.error(err instanceof ApiError ? err.detail : 'Não foi possível atualizar o pedido.');
    } finally {
      setUpdatingId(null);
    }
  }

  return (
    <section>
      <div className="mb-8"><h1 className="text-3xl font-bold">Pedidos</h1><p className="mt-2 text-zinc-400">Acompanhe e atualize os pedidos da loja.</p></div>

      {loading ? <div className="py-16 text-center text-zinc-400">Carregando pedidos...</div> : orders.length === 0 ? <div className="rounded-2xl border border-white/10 bg-[#111113] p-8 text-zinc-400">Nenhum pedido encontrado.</div> : (
        <div className="overflow-hidden rounded-2xl border border-white/10 bg-[#111113]">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[800px] text-left text-sm">
              <thead className="border-b border-white/10 text-zinc-500"><tr><th className="px-5 py-4">Pedido</th><th className="px-5 py-4">Usuário</th><th className="px-5 py-4">Data</th><th className="px-5 py-4">Total</th><th className="px-5 py-4">Status</th></tr></thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.id} className="border-b border-white/5 last:border-0">
                    <td className="px-5 py-4 font-medium">#{order.id}</td>
                    <td className="px-5 py-4 text-zinc-400">#{order.userId}</td>
                    <td className="px-5 py-4 text-zinc-400">{new Date(order.createdAt).toLocaleString('pt-BR')}</td>
                    <td className="px-5 py-4">{formatCurrency(order.total)}</td>
                    <td className="px-5 py-4"><select disabled={updatingId === order.id} value={order.status} onChange={(event) => void handleStatusChange(order.id, event.target.value as OrderStatus)} className="rounded-lg border border-white/10 bg-[#09090B] px-3 py-2 outline-none focus:border-[#8257E5]">{statuses.map((status) => <option key={status} value={status}>{status}</option>)}</select></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </section>
  );
}
