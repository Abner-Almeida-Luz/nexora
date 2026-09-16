import { useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'react-hot-toast';
import { ApiError } from '../../services/api';
import { categoriesApi } from '../../services/categoryService';
import type { Category} from '../../types';

export const productSchema = z.object({
  name: z.string().trim().min(2, 'Informe o nome do produto.'),
  description: z.string().trim().transform((value) => value || null),
  price: z.coerce.number().positive('O preço deve ser maior que zero.'),
  stock: z.coerce.number().int().min(0, 'O estoque não pode ser negativo.'),
  imageUrl: z.union([
    z.string().url('URL de imagem inválida.'),
    z.literal(''),
  ]).transform((value) => value || null),
  categoryId: z.coerce.number().int().positive('Selecione uma categoria.'),
});

export type ProductFormValues = z.infer<typeof productSchema>;

interface ProductFormProps {
  initialValues?: Partial<ProductFormValues>;
  submitLabel: string;
  onSubmit: (data: ProductFormValues) => Promise<void>;
}

export function ProductForm({ initialValues, submitLabel, onSubmit }: ProductFormProps) {
  const [categories, setCategories] = useState<Category[]>([]);
  const [serverErrors, setServerErrors] = useState<Record<string, string>>({});

  const defaultValues = useMemo<ProductFormValues>(() => ({
    name: initialValues?.name ?? '',
    description: initialValues?.description ?? null,
    price: initialValues?.price ?? 0,
    stock: initialValues?.stock ?? 0,
    imageUrl: initialValues?.imageUrl ?? null,
    categoryId: initialValues?.categoryId ?? 0,
  }), [initialValues]);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ProductFormValues>({
    resolver: zodResolver(productSchema),
    defaultValues,
  });

  useEffect(() => {
    reset(defaultValues);
  }, [defaultValues, reset]);

  useEffect(() => {
    void categoriesApi.list().then(setCategories).catch((error) => {
      toast.error(error instanceof ApiError ? error.detail : 'Não foi possível carregar categorias.');
    });
  }, []);

  async function submit(data: ProductFormValues) {
    setServerErrors({});
    try {
      await onSubmit(data);
    } catch (error) {
      if (error instanceof ApiError) {
        setServerErrors(error.errors ?? {});
        if (!error.errors) toast.error(error.detail);
        return;
      }
      toast.error('Não foi possível salvar o produto.');
    }
  }

  return (
    <form onSubmit={handleSubmit(submit)} className="mt-8 space-y-5 rounded-2xl border border-white/10 bg-[#111113] p-6">
      <label className="block text-sm">
        Nome
        <input {...register('name')} className="mt-2 w-full rounded-xl border border-white/10 bg-[#09090B] px-4 py-3 outline-none focus:border-[#8257E5]" />
        {errors.name && <p className="mt-1 text-sm text-red-400">{errors.name.message}</p>}
        {serverErrors.name && <p className="mt-1 text-sm text-red-400">{serverErrors.name}</p>}
      </label>

      <label className="block text-sm">
        Descrição
        <textarea {...register('description')} rows={4} className="mt-2 w-full rounded-xl border border-white/10 bg-[#09090B] px-4 py-3 outline-none focus:border-[#8257E5]" />
        {serverErrors.description && <p className="mt-1 text-sm text-red-400">{serverErrors.description}</p>}
      </label>

      <div className="grid gap-5 sm:grid-cols-2">
        <label className="block text-sm">
          Preço
          <input {...register('price')} type="number" step="0.01" min="0" className="mt-2 w-full rounded-xl border border-white/10 bg-[#09090B] px-4 py-3 outline-none focus:border-[#8257E5]" />
          {errors.price && <p className="mt-1 text-sm text-red-400">{errors.price.message}</p>}
          {serverErrors.price && <p className="mt-1 text-sm text-red-400">{serverErrors.price}</p>}
        </label>

        <label className="block text-sm">
          Estoque
          <input {...register('stock')} type="number" min="0" className="mt-2 w-full rounded-xl border border-white/10 bg-[#09090B] px-4 py-3 outline-none focus:border-[#8257E5]" />
          {errors.stock && <p className="mt-1 text-sm text-red-400">{errors.stock.message}</p>}
          {serverErrors.stock && <p className="mt-1 text-sm text-red-400">{serverErrors.stock}</p>}
        </label>
      </div>

      <label className="block text-sm">
        Categoria
        <select {...register('categoryId')} className="mt-2 w-full rounded-xl border border-white/10 bg-[#09090B] px-4 py-3 outline-none focus:border-[#8257E5]">
          <option value="0">Selecione</option>
          {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
        </select>
        {errors.categoryId && <p className="mt-1 text-sm text-red-400">{errors.categoryId.message}</p>}
        {serverErrors.categoryId && <p className="mt-1 text-sm text-red-400">{serverErrors.categoryId}</p>}
      </label>

      <label className="block text-sm">
        URL da imagem
        <input {...register('imageUrl')} type="url" className="mt-2 w-full rounded-xl border border-white/10 bg-[#09090B] px-4 py-3 outline-none focus:border-[#8257E5]" />
        {errors.imageUrl && <p className="mt-1 text-sm text-red-400">{errors.imageUrl.message}</p>}
        {serverErrors.imageUrl && <p className="mt-1 text-sm text-red-400">{serverErrors.imageUrl}</p>}
      </label>

      <button disabled={isSubmitting} className="w-full rounded-xl bg-[#8257E5] px-5 py-3 font-medium hover:bg-[#7047D0] disabled:cursor-not-allowed disabled:opacity-50">
        {isSubmitting ? 'Salvando...' : submitLabel}
      </button>
    </form>
  );
}
