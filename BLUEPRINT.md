# NEXORA — Blueprint

> Plano consolidado do projeto. Muda raramente, apenas quando o escopo evolui.
> Complementa `CLAUDE.md` (estado vivo) e `DEPLOY.md` (runbook).

## Objetivo

E-commerce full-stack. Backend REST stateless com autenticação JWT,
carrinho persistido, checkout transacional, reviews com restrição única.
Frontend SPA consumindo a API em tempo real (sem mocks).

Projeto de estudo full-stack, não é um produto comercial.

## Identidade

- Produto: **NEXORA**
- Repositório: https://github.com/Abner-Almeida-Luz/nexora
- Backend: `com.diversao.backend` (Spring Boot 3.3.2 / Java 21)
- Frontend: React 19 + TypeScript strict + Vite + React Router 7 + Tailwind

## Estrutura de repositório

Monorepo com duas pastas na raiz:

```
nexora/
├── backend/        → deploy no Railway
├── frontend/       → deploy no Vercel
├── docs/           → ADRs e guias
├── README.md       → visão geral
├── CLAUDE.md       → estado atual
├── BLUEPRINT.md    → este arquivo
└── DEPLOY.md       → runbook de produção
```

Cada plataforma (Railway, Vercel) aponta "Root Directory" para sua pasta
respectiva. Isso mantém o código junto durante o desenvolvimento e permite
deploys independentes.

## Modelo de dados

### users

`id`, `name`, `email` (único), `password` (BCrypt), `role` (USER | ADMIN),
`created_at`

### categories

`id`, `name` (único)

### products

`id`, `name`, `description`, `price` (> 0), `stock` (>= 0), `image_url`,
`category_id` (FK), `version` (lock otimista)

### carts

`id`, `user_id` (único — um carrinho por usuário)

### cart_items

`id`, `cart_id`, `product_id`, `quantity` (> 0)

### orders

`id`, `user_id`, `status` (PENDING | PAID | SHIPPED | DELIVERED | CANCELLED),
`created_at`, `total`

### order_items

`id`, `order_id`, `product_id`, `product_name` (snapshot),
`price` (preço congelado no checkout), `quantity`

### reviews

`id`, `product_id`, `user_id`, `rating` (1..5), `comment` (até 1000),
`created_at`
**Constraint única:** `(product_id, user_id)` — no banco, não só no service.

## Endpoints da API

Todas as respostas de erro seguem `ProblemDetail` (RFC 7807):
`{ type, title, status, detail, timestamp, errors? }`.
O campo `errors` só existe em 400 de validação de formulário
(mapa campo → mensagem).

### Auth (`/api/auth`) — público

| Método | Path | Corpo | Resposta |
|---|---|---|---|
| POST | `/register` | `{ name, email, password }` | `{ token, refreshToken, email, name, role }` |
| POST | `/login` | `{ email, password }` | idem |
| POST | `/refresh` | `{ refreshToken }` | idem |

### Categories

| Método | Path | Role |
|---|---|---|
| GET | `/api/categories` | público |
| GET | `/api/categories/{id}` | público |
| POST | `/api/categories` | ADMIN |
| PUT | `/api/categories/{id}` | ADMIN |
| DELETE | `/api/categories/{id}` | ADMIN (409 se tiver produtos) |

### Products

| Método | Path | Role |
|---|---|---|
| GET | `/api/products?search=&categoryId=&page=&size=` | público |
| GET | `/api/products/{id}` | público |
| POST | `/api/products` | ADMIN |
| PUT | `/api/products/{id}` | ADMIN |
| DELETE | `/api/products/{id}` | ADMIN |
| GET | `/api/products/{id}/reviews?page=&size=` | público |

### Cart

| Método | Path | Auth |
|---|---|---|
| GET | `/api/cart` | autenticado |
| POST | `/api/cart/items` | autenticado |
| PUT | `/api/cart/items/{productId}?quantity=N` | autenticado (0 remove) |
| DELETE | `/api/cart` | autenticado |

### Orders

| Método | Path | Role |
|---|---|---|
| POST | `/api/orders` | autenticado |
| GET | `/api/orders` | autenticado |
| GET | `/api/orders/{id}` | dono ou ADMIN |
| PATCH | `/api/orders/{id}/cancel` | dono, só se PENDING |
| GET | `/api/orders/admin/all` | ADMIN |
| PATCH | `/api/orders/{id}/status` | ADMIN |

### Reviews

| Método | Path | Auth |
|---|---|---|
| POST | `/api/reviews` | autenticado |
| DELETE | `/api/reviews/{id}` | autor ou ADMIN |

## Regras de negócio que não podem ser quebradas

1. Estoque só diminui dentro da transação de criação de pedido. Falha → rollback total.
2. Carrinho nunca congela preço. Pedido **sempre** congela em `order_items`.
3. Um usuário só vê/cancela os próprios pedidos. ADMIN vê todos.
4. Email duplicado no registro → 409, nunca erro de banco vazando.
5. Review é única por `(product_id, user_id)` — constraint no banco.
6. Cancelamento de pedido só se `status == PENDING`. Devolve estoque na mesma transação.
7. Rate limit de login: 5 tentativas / 15 min por **email** (não IP).
8. Refresh token nunca autentica como access token (claim `type=refresh`
   rejeitado no filtro).

## Decisões de arquitetura

### Backend

- **JWT stateless** em vez de sessão — escala horizontal sem store compartilhado.
- **Flyway + `ddl-auto: validate`** — Hibernate nunca altera schema em prod.
- **Estrutura por domínio**, não por camada. DTOs e mappers dentro do pacote
  do domínio (estrutura achatada, sem subpacotes `.dto` / `.mapper`).
- **Lock otimista** (`@Version` em Product) + tradução de
  `ObjectOptimisticLockingFailureException` → 409.
- **Testcontainers** em vez de H2 nos testes de integração — H2 mascara bugs
  específicos do Postgres (ex.: `lower(bytea)`).
- **Cache em memória** (`ConcurrentMapCacheManager`) para `products` e
  `categories`. Migração para Caffeine planejada.
- **Erros centralizados** via `GlobalExceptionHandler` + `ProblemDetail`.
  Mensagens em `ErrorMessages` (sem strings mágicas).

### Frontend

- **AuthContext** com persistência de token em `localStorage` (exceção
  deliberada à regra de não usar `localStorage` para estado de negócio).
- **`<ProtectedRoute>`** para rotas autenticadas; `<AdminRoute>` para ADMIN.
- **Camada de API** fina em `src/api/` — um arquivo por domínio, sem lógica
  de negócio.
- **Estados explícitos** de loading/erro em toda página que busca dados.

### Deploy

- **Backend → Railway**; **frontend → Vercel**; ambos conectados ao mesmo
  monorepo do GitHub com "Root Directory" apontando para a pasta respectiva.
- **Config por variável de ambiente** (12-factor). Nenhuma URL ou segredo
  hardcoded.
- **CORS explícito** via `CORS_ALLOWED_ORIGINS` no Railway. Nunca `*` com
  `allowCredentials: true`.
- **Admin via SQL manual** — sem seed em prod, sem endpoint de bootstrap.

## Fora de escopo (decidido não fazer)

- Upload real de imagem (só `imageUrl` textual)
- Pagamento real (Order só muda de status manualmente pelo ADMIN)
- Notificações por email
- Multi-tenant
- Rate limit distribuído (Redis) — só em memória local por ora
- Rotação invalidante de refresh token (registrado como débito)
- i18n / multi-idioma
- PWA / offline
- SSR / SSG

## Design system (fechado)

- Fundo: `#0F0F13`
- Destaque: `#8257E5` (hover `#9466ff`)
- Bordas: `border-white/10`
- Cantos: `rounded-lg`
- Header sticky com `backdrop-blur`
- Tipografia: tracking largo em títulos; texto secundário `zinc-400`/`zinc-500`