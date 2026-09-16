import { Outlet } from 'react-router-dom';
import Navbar from '../Navbar';

export function MainLayout() {
  return (
    <div className="min-h-dvh bg-[#09090B] text-white">
      <Navbar />
      <main className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <Outlet />
      </main>
    </div>
  );
}
