import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/useAuth';
import { ApiError } from '../services/api';
import { FieldError } from '../components/ui/FieldError';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function validate() {
    const next: Record<string, string> = {};
    if (!email.trim()) next.email = 'Informe o e-mail.';
    else if (!/^\S+@\S+\.\S+$/.test(email)) next.email = 'Informe um e-mail válido.';
    if (!password) next.password = 'Informe a senha.';
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setGeneralError(null);
    setErrors({});
    if (!validate()) return;
    try {
      setLoading(true);
      await login(email.trim(), password);
      const target = (location.state as { from?: string } | null)?.from ?? '/';
      navigate(target, { replace: true });
    } catch (error) {
      if (error instanceof ApiError) {
        setErrors(error.errors ?? {});
        if (!error.errors) setGeneralError(error.detail);
      } else setGeneralError('Não foi possível realizar o login.');
    } finally { setLoading(false); }
  }

  return <section className="mx-auto flex min-h-[70vh] max-w-md items-center"><form onSubmit={handleSubmit} noValidate className="w-full rounded-2xl border border-white/10 bg-[#111113] p-6 sm:p-8"><h1 className="text-3xl font-bold">Entrar</h1><p className="mt-2 text-sm text-zinc-400">Acesse sua conta Nexora.</p>{generalError && <div className="mt-6 rounded-xl border border-red-500/20 bg-red-500/10 p-3 text-sm text-red-300">{generalError}</div>}<label className="mt-6 block text-sm text-zinc-300">E-mail<input value={email} onChange={(e) => setEmail(e.target.value)} type="email" className="mt-2 w-full rounded-xl border border-white/10 bg-[#09090B] px-4 py-3 outline-none focus:border-[#8257E5]" /> <FieldError message={errors.email}/></label><label className="mt-5 block text-sm text-zinc-300">Senha<input value={password} onChange={(e) => setPassword(e.target.value)} type="password" className="mt-2 w-full rounded-xl border border-white/10 bg-[#09090B] px-4 py-3 outline-none focus:border-[#8257E5]" /> <FieldError message={errors.password}/></label><button disabled={loading} className="mt-6 w-full rounded-xl bg-[#8257E5] px-5 py-3 font-medium disabled:opacity-50">{loading ? 'Entrando...' : 'Entrar'}</button><p className="mt-6 text-center text-sm text-zinc-500">Não possui conta? <Link to="/register" className="text-[#B99AFF]">Criar conta</Link></p></form></section>;
}
