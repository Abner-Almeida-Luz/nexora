import { ShoppingCart } from 'lucide-react';
import { Link } from 'react-router-dom';
import { formatCurrency } from '../../utils/currency';
import type { Product } from '../../types';

interface ProductCardProps {
  product: Product;
  onAddToCart?: (productId: number) => void;
  adding?: boolean;
}

export function ProductCard({ product, onAddToCart, adding = false }: ProductCardProps) {
  const outOfStock = product.stock <= 0;

  return (
    <article className="group overflow-hidden rounded-2xl border border-white/10 bg-[#111113]">
      <Link to={`/products/${product.id}`} className="block aspect-square overflow-hidden bg-zinc-900">
        {product.imageUrl ? <img src={product.imageUrl} alt={product.name} className="h-full w-full object-cover transition duration-500 group-hover:scale-105" /> : <div className="flex h-full items-center justify-center text-sm text-zinc-600">Sem imagem</div>}
      </Link>
      <div className="p-5">
        <p className="text-xs font-medium uppercase tracking-wider text-[#9A6CFF]">{product.categoryName}</p>
        <Link to={`/products/${product.id}`}><h2 className="mt-2 text-lg font-semibold hover:text-[#B99AFF]">{product.name}</h2></Link>
        <p className="mt-2 line-clamp-2 text-sm text-zinc-400">{product.description ?? 'Sem descrição.'}</p>
        <div className="mt-5 flex items-center justify-between gap-3">
          <strong className="text-lg">{formatCurrency(product.price)}</strong>
          {onAddToCart && <button type="button" onClick={() => onAddToCart(product.id)} disabled={outOfStock || adding} className="inline-flex items-center gap-2 rounded-xl bg-[#8257E5] px-3 py-2 text-sm font-medium hover:bg-[#7047D0] disabled:cursor-not-allowed disabled:opacity-50"><ShoppingCart size={16} />{adding ? 'Adicionando...' : outOfStock ? 'Esgotado' : 'Comprar'}</button>}
        </div>
      </div>
    </article>
  );
}
