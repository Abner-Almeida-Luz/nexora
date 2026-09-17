# NEXORA — Contexto

> Última atualização: 2026-09-16
> Este arquivo é a fonte de verdade para o estado atual do projeto.
> Complementa `BLUEPRINT.md` (plano) e `DEPLOY.md` (runbook).

## Identidade

- Produto: **NEXORA** (nome comercial fixo)
- Monorepo com duas pastas: `backend/` e `frontend/`
- Pacote raiz do backend: `com.diversao.backend` (nome histórico, mantido)

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Spring Boot 3.3.2 / Java 21 |
| Segurança | JWT stateless (access + refresh) |
| Banco prod | PostgreSQL 16 (Railway) |
| Banco dev | H2 em memória |
| Banco test | PostgreSQL via Testcontainers |
| Migrations | Flyway (V1, V2 aplicadas) |
| Frontend | React 19 + TS strict + Vite + React Router 7 + Tailwind |
| Deploy backend | Railway |
| Deploy frontend | Vercel |

## Restrições rígidas

- Nunca `ddl-auto: update` em prod. Sempre `validate` + Flyway.
- Nunca `JWT_SECRET` hardcoded. Sempre variável de ambiente sem default.
- Nunca `permitAll()` sem filtro de método HTTP em `/api/products/**` e
  `/api/categories/**`. Regras por método vêm antes da leitura pública.
- Nunca `any` no TypeScript do frontend. Sem `@ts-ignore` sem justificativa.
- Nunca `localStorage` fora do token (`token`, `refreshToken`, `user`).
- Nunca duplicar markup entre páginas — reaproveitar `ProductCard`,
  `ProductList`, `SectionTitle` etc.
- Paleta do design system fechada: fundo `#0F0F13`, destaque `#8257E5`.
- Nunca strings de erro literais nos services. Sempre `ErrorMessages.X`.

## Comandos

```bash
# Backend — dev
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Backend — testes
cd backend && mvn test

# Frontend — dev
cd frontend && npm run dev

# Frontend — build
cd frontend && npm run build
```

## Estado atual

### Funcionando

- CRUD de produtos e categorias com restrição de role
- Carrinho → pedido com baixa transacional de estoque
- Cancelamento de pedido pelo dono (se PENDING)
- Reviews com constraint única (product_id, user_id)
- JWT access + refresh; rate limit no login (5/15min por email)
- Auditoria leve via `AuditLogger` (SLF4J)
- Cache em `products` e `categories` (ConcurrentMapCacheManager)
- Migrations V1 (schema) e V2 (version para lock otimista em Product)
- Testes unitários (todos os Services) + integração (todos os Controllers)
- Frontend: Home, Products, ProductDetails, Cart, Checkout, Profile, Login,
  Register; Blog estático
- Deploy: backend no Railway, frontend no Vercel, CORS liberado

### Incompleto

- Painel ADMIN no frontend: apenas algumas telas (`AdminProducts`,
  `AdminProductCreate`, `AdminProductEdit`, `AdminCategories`, `AdminOrders`)
  estão presentes, mas o fluxo ainda precisa ser testado ponta a ponta
- Fluxo de review no frontend precisa ser testado contra produção

### Não iniciado

- Rotação invalidante de refresh token
- Cache com TTL (migrar para Caffeine)
- Testes automatizados do frontend
- Toast global de feedback no frontend
- `Intl.NumberFormat` para formatação de preço

## Próximos passos (ordenados)

1. Testar painel ADMIN ponta a ponta contra produção
2. Logout automático em 401 no frontend (handler centralizado)
3. `Intl.NumberFormat` para preços no frontend
4. Cobertura de testes do frontend (Vitest + Testing Library)
5. Migrar cache para Caffeine com TTL
6. Rotação de refresh token com tabela `used_refresh_tokens`

## Débito técnico conhecido

- `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` no Railway são valores literais
  copiados do card Postgres, não referências `${{Postgres.*}}`. Se o banco
  for recriado, atualizar manualmente.
- `LoginRateLimiter` sem TTL nem limite de tamanho do mapa em memória.
- `UserService.getByEmail` lança `BadRequestException` (400) em vez de
  `ResourceNotFoundException` (404).
- `handleUnauthorized` no `client.ts` faz `window.location.assign('/login')`,
  forçando reload completo do SPA.
- CI existe (GitHub Actions roda `mvn test` e `npm run build`), mas **não
  bloqueia deploy**. Push na `main` deploya direto em Railway e Vercel,
  mesmo se os testes falharem.
- Sem monitoramento. Erros só aparecem se alguém abrir os logs.

## Deploy

- **Backend:** https://nexora-production-1de3.up.railway.app
  - Railway, Root Directory: `backend`, perfil `prod`, Postgres no mesmo projeto
- **Frontend:** https://nexora-hazel-zeta.vercel.app
  - Vercel, Root Directory: `frontend`, env `VITE_API_BASE_URL`
- **CORS:** `CORS_ALLOWED_ORIGINS` no Railway = URL do Vercel
- **Conta ADMIN em produção:** `admin@diversao.com`
- Runbook completo: [`DEPLOY.md`](DEPLOY.md)

## Documentação

- [`README.md`](README.md) — visão geral e como rodar
- [`BLUEPRINT.md`](BLUEPRINT.md) — plano e decisões
- [`DEPLOY.md`](DEPLOY.md) — operação
- [`docs/`](docs/) — ADRs e guias

## Log de sessões

> Formato: `YYYY-MM-DD — o que foi feito / próxima ação`
> Ordem cronológica inversa (mais recente no topo).

- **2026-09-16** — Primeiro deploy funcional. Backend no Railway, frontend
  no Vercel, CORS liberado. Conta ADMIN criada via SQL no Postgres do Railway.
  Testado: registro, login, listagem de produtos e categorias.
  Próxima ação: testar painel ADMIN ponta a ponta.

<!-- Novas entradas ACIMA desta linha -->