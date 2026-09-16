import { Link, NavLink, useNavigate } from 'react-router-dom';
import { ShoppingCart, UserRound } from 'lucide-react';
import { useAuth } from '../contexts/useAuth';

const linkClass = ({ isActive }: { isActive: boolean }) => `text-sm transition ${isActive ? 'text-white' : 'text-zinc-400 hover:text-white'}`;

export default function Navbar() {
  const { isAuthenticated, isAdmin, user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <header className="sticky top-0 z-40 border-b border-white/10 bg-[#09090B]/90 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-6 px-4 sm:px-6 lg:px-8">
        <Link to="/" className="text-xl font-bold tracking-tight text-[#8257E5]">Nexora</Link>
        <nav className="hidden items-center gap-6 md:flex">
          <NavLink to="/" className={linkClass}>Início</NavLink>
          <NavLink to="/products" className={linkClass}>Produtos</NavLink>
          <NavLink to="/blog" className={linkClass}>Blog</NavLink>
          {isAdmin && <NavLink to="/admin/products" className={linkClass}>Admin</NavLink>}
        </nav>
        <div className="flex items-center gap-2">
          {isAuthenticated ? <>
            <span className="hidden text-sm text-zinc-400 lg:inline">Olá, {user?.name}</span>
            <Link to="/cart" className="rounded-lg p-2 text-zinc-400 hover:bg-white/5 hover:text-white" aria-label="Carrinho"><ShoppingCart size={19} /></Link>
            <Link to="/profile" className="rounded-lg p-2 text-zinc-400 hover:bg-white/5 hover:text-white" aria-label="Perfil"><UserRound size={19} /></Link>
            <button onClick={handleLogout} className="rounded-lg px-3 py-2 text-sm text-zinc-400 hover:bg-white/5 hover:text-white">Sair</button>
          </> : <Link to="/login" className="rounded-lg bg-[#8257E5] px-4 py-2 text-sm font-medium text-white hover:bg-[#7047D0]">Entrar</Link>}
        </div>
      </div>
    </header>
  );
}
