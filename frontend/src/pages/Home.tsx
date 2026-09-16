import { ArrowRight, ShieldCheck, Sparkles, Truck } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function Home() {
  return (
    <div className="space-y-20">
      <section className="relative overflow-hidden rounded-3xl border border-white/10 bg-gradient-to-br from-[#17121F] via-[#111113] to-[#09090B] px-6 py-20 sm:px-10 lg:px-16">
        <div className="relative z-10 max-w-3xl">
          <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-[#8257E5]/30 bg-[#8257E5]/10 px-4 py-2 text-sm text-[#B99AFF]">
            <Sparkles size={16} /> Tecnologia para seu próximo nível
          </div>
          <h1 className="text-4xl font-bold leading-tight tracking-tight sm:text-5xl lg:text-6xl">
            Produtos que acompanham <span className="text-[#8257E5]">seu ritmo.</span>
          </h1>
          <p className="mt-6 max-w-2xl text-lg leading-8 text-zinc-400">
            Uma loja moderna para tecnologia, estudo, trabalho e entretenimento.
          </p>
          <Link to="/products" className="mt-8 inline-flex items-center gap-2 rounded-xl bg-[#8257E5] px-6 py-3 font-medium hover:bg-[#7047D0]">
            Explorar produtos <ArrowRight size={18} />
          </Link>
        </div>
      </section>

      <section className="grid gap-6 md:grid-cols-3">
        <div className="rounded-2xl border border-white/10 bg-[#111113] p-6">
          <ShieldCheck className="text-[#9A6CFF]" />
          <h2 className="mt-4 font-semibold">Compra segura</h2>
          <p className="mt-2 text-sm text-zinc-400">Autenticação e pedidos vinculados à sua conta.</p>
        </div>
        <div className="rounded-2xl border border-white/10 bg-[#111113] p-6">
          <Truck className="text-[#9A6CFF]" />
          <h2 className="mt-4 font-semibold">Acompanhamento</h2>
          <p className="mt-2 text-sm text-zinc-400">Consulte seus pedidos e respectivos status.</p>
        </div>
        <div className="rounded-2xl border border-white/10 bg-[#111113] p-6">
          <Sparkles className="text-[#9A6CFF]" />
          <h2 className="mt-4 font-semibold">Experiência moderna</h2>
          <p className="mt-2 text-sm text-zinc-400">Frontend tipado e conectado a uma API Spring Boot real.</p>
        </div>
      </section>
    </div>
  );
}
