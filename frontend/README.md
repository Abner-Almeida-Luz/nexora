# Nexora Frontend

React 19 + TypeScript + Vite + React Router + Tailwind CSS frontend connected to the Spring Boot `/api` backend.

## Stack

- React 19
- TypeScript
- Vite
- React Router
- Tailwind CSS
- Fetch API (no Axios/interceptors/cache in the HTTP client)
- React Hook Form + Zod
- React Hot Toast
- Lucide React
- Vitest + Testing Library
- Oxlint + Prettier

## Start

```bash
npm install
npm run dev
```

The Vite proxy forwards `/api` to `http://localhost:8080`.

## Test and quality

```bash
npm test
npm run lint
npm run format:check
npm run build
```
