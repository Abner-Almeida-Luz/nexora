import { Link } from 'react-router-dom';

const posts = [
  { id: 1, title: 'Como escolher um notebook para estudar', description: 'Os principais componentes para equilibrar desempenho e custo.' },
  { id: 2, title: 'SSD, RAM e CPU: o que realmente importa?', description: 'Entenda o papel de cada componente no desempenho.' },
  { id: 3, title: 'Como montar um setup produtivo', description: 'Organização, ergonomia e foco sem precisar gastar muito.' },
];

export default function Blog() {
  return <section><h1 className="text-3xl font-bold">Blog</h1><p className="mt-2 text-zinc-400">Conteúdo sobre tecnologia e produtividade.</p><div className="mt-8 grid gap-6 md:grid-cols-3">{posts.map((post) => <article key={post.id} className="rounded-2xl border border-white/10 bg-[#111113] p-6"><div className="aspect-video rounded-xl bg-zinc-900"/><h2 className="mt-5 text-xl font-semibold">{post.title}</h2><p className="mt-2 text-sm leading-6 text-zinc-400">{post.description}</p><Link to={`/blog/${post.id}`} className="mt-5 inline-block text-sm text-[#B99AFF]">Ler artigo →</Link></article>)}</div></section>;
}
