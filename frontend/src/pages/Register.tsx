import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/useAuth';
import { ApiError } from '../services/api';
import { FieldError } from '../components/ui/FieldError';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function validate() {
    const next: Record<string, string> = {};
    if (!name.trim()) next.name = 'Informe seu nome.';
    if (!email.trim()) next.email = 'Informe o e-mail.';
    else if (!/^\S+@\S+\.\S+$/.test(email)) next.email = 'Informe um e-mail válido.';
    if (!password) next.password = 'Informe uma senha.';
    else if (password.length < 6) next.password = 'A senha deve possuir pelo menos 6 caracteres.';
    if (password !== confirmPassword) next.confirmPassword = 'As senhas não coincidem.';
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault(); setErrors({}); setGeneralError(null); if (!validate()) return;
    try { setLoading(true); await register(name.trim(), email.trim(), password); navigate('/', { replace: true }); }
    catch (error) { if (error instanceof ApiError) { setErrors(error.errors ?? {}); if (!error.errors) setGeneralError(error.detail); } else setGeneralError('Não foi possível criar a conta.'); }
    finally { setLoading(false); }
  }

  return <section className="mx-auto max-w-md py-10"><form onSubmit={handleSubmit} noValidate className="rounded-2xl border border-white/10 bg-[#111113] p-6 sm:p-8"><h1 className="text-3xl font-bold">Criar conta</h1><p className="mt-2 text-sm text-zinc-400">Cadastre-se para comprar e acompanhar pedidos.</p>{generalError && <div className="mt-6 rounded-xl border border-red-500/20 bg-red-500/10 p-3 text-sm text-red-300">{generalError}</div>}<label className="mt-6 block text-sm">Nome<input value={name} onChange={(e) => setName(e.target.value)} className="mt-2 w-full rounded-xl border border-white/10 bg-[#09090B] px-4 py-3 outline-none focus:border-[#8257E5]" /> <FieldError message={errors.name}/></label><label className="mt-5 block text-sm">E-mail<input value={email} onChange={(e) => setEmail(e.target.value)} type="email" className="mt-2 w-full rounded-xl border border-white/10 bg-[#09090B] px-4 py-3 outline-none focus:border-[#8257E5]" /> <FieldError message={errors.email}/></label><label className="mt-5 block text-sm">Senha<input value={password} onChange={(e) => setPassword(e.target.value)} type="password" className="mt-2 w-full rounded-xl border border-white/10 bg-[#09090B] px-4 py-3 outline-none focus:border-[#8257E5]" /> <FieldError message={errors.password}/></label><label className="mt-5 block text-sm">Confirmar senha<input value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} type="password" className="mt-2 w-full rounded-xl border border-white/10 bg-[#09090B] px-4 py-3 outline-none focus:border-[#8257E5]" /> <FieldError message={errors.confirmPassword}/></label><button disabled={loading} className="mt-6 w-full rounded-xl bg-[#8257E5] px-5 py-3 font-medium disabled:opacity-50">{loading ? 'Criando...' : 'Criar conta'}</button><p className="mt-6 text-center text-sm text-zinc-500">Já possui conta? <Link to="/login" className="text-[#B99AFF]">Entrar</Link></p></form></section>;
}
