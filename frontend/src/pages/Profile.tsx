import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/useAuth';

export default function Profile() {
  const { user, isAdmin } = useAuth();

  return <section className="mx-auto max-w-3xl"><h1 className="text-3xl font-bold">Meu perfil</h1><p className="mt-2 text-zinc-400">Informações disponíveis localmente a partir do login/registro.</p><div className="mt-8 rounded-2xl border border-white/10 bg-[#111113] p-6"><dl className="space-y-5"><div><dt className="text-sm text-zinc-500">Nome</dt><dd className="mt-1 font-medium">{user?.name}</dd></div><div><dt className="text-sm text-zinc-500">E-mail</dt><dd className="mt-1 font-medium">{user?.email}</dd></div><div><dt className="text-sm text-zinc-500">Perfil</dt><dd className="mt-1 font-medium">{user?.role}</dd></div></dl><div className="mt-8 flex flex-wrap gap-3"><Link to="/orders" className="rounded-xl border border-white/10 px-4 py-2 text-sm hover:bg-white/5">Meus pedidos</Link>{isAdmin && <Link to="/admin/products/new" className="rounded-xl bg-[#8257E5] px-4 py-2 text-sm">Admin</Link>}</div></div></section>;
}
