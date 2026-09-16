import { toast } from 'react-hot-toast';
import { useNavigate } from 'react-router-dom';
import { ProductForm, type ProductFormValues } from '../components/products/ProductForm';
import { productsApi } from '../services/productService';

export default function AdminProductCreate() {
  const navigate = useNavigate();

  async function handleSubmit(data: ProductFormValues) {
    await productsApi.create(data);
    toast.success('Produto criado.');
    navigate('/admin/products');
  }

  return (
    <section className="mx-auto max-w-2xl">
      <h1 className="text-3xl font-bold">Novo produto</h1>
      <p className="mt-2 text-zinc-400">Área administrativa.</p>
      <ProductForm submitLabel="Criar produto" onSubmit={handleSubmit} />
    </section>
  );
}
