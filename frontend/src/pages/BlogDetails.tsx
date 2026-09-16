import { Link, useParams } from 'react-router-dom';

const content: Record<string, { title: string; paragraphs: string[] }> = {
  '1': { title: 'Como escolher um notebook para estudar', paragraphs: ['Priorize equilíbrio entre CPU, RAM e armazenamento.', 'Também considere bateria, teclado e qualidade da tela.', 'Para programação e produtividade, 16 GB de RAM costuma oferecer uma experiência confortável.'] },
  '2': { title: 'SSD, RAM e CPU: o que realmente importa?', paragraphs: ['A CPU executa as instruções, a RAM mantém dados em uso e o SSD armazena dados de forma persistente.', 'O melhor equipamento é o que equilibra esses componentes de acordo com seu uso.'] },
  '3': { title: 'Como montar um setup produtivo', paragraphs: ['Ergonomia, iluminação e organização influenciam mais do que simplesmente adicionar equipamentos.', 'Comece pela cadeira, posição do monitor e iluminação antes de investir em acessórios.'] },
};

export default function BlogDetails() {
  const { id } = useParams();
  const post = id ? content[id] : undefined;
  if (!post) return <div className="rounded-2xl border border-white/10 bg-[#111113] p-10 text-center text-zinc-400">Artigo não encontrado.</div>;
  return <article className="mx-auto max-w-3xl"><Link to="/blog" className="text-sm text-zinc-400 hover:text-white">← Voltar</Link><div className="mt-6 aspect-video rounded-3xl bg-zinc-900"/><h1 className="mt-8 text-4xl font-bold leading-tight">{post.title}</h1><div className="mt-8 space-y-6">{post.paragraphs.map((paragraph) => <p key={paragraph} className="text-lg leading-8 text-zinc-400">{paragraph}</p>)}</div></article>;
}
