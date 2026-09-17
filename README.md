# NEXORA

E-commerce full-stack. Backend em Spring Boot, frontend em React.

## O que é

- **Backend:** API REST stateless com JWT, Spring Boot 3.3 / Java 21.
- **Frontend:** SPA em React 19 + TypeScript + Vite + React Router 7 + Tailwind.
- **Banco:** PostgreSQL 16 em produção, H2 em desenvolvimento.
- **Repositório:** https://github.com/Abner-Almeida-Luz/nexora

Projeto de estudo full-stack, não é um produto comercial.

## Rodar localmente

Pré-requisitos: JDK 21, Node 20+, Docker (para testes de integração).

### Backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Sobe em `http://localhost:8080` com H2 em memória.

- Swagger: `http://localhost:8080/swagger-ui.html`
- H2 Console: `http://localhost:8080/h2-console`

Credenciais de dev (perfil `dev`):

- ADMIN: `admin@diversao.com` / `admin123`
- USER: `cliente@diversao.com` / `cliente123`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Sobe em `http://localhost:5173`, com proxy `/api` para `http://localhost:8080`.

## Testes

```bash
# Backend (requer Docker — Testcontainers)
cd backend && mvn test

# Backend — só unitários, sem Docker
cd backend && mvn test -Dgroups=unit

# Frontend (quando configurado)
cd frontend && npm test
```

## Produção

- Frontend: https://nexora-hazel-zeta.vercel.app
- Backend: https://nexora-production-1de3.up.railway.app
- Swagger: https://nexora-production-1de3.up.railway.app/swagger-ui/index.html

Runbook completo de operação: [`DEPLOY.md`](DEPLOY.md).

## Documentação

- [`CLAUDE.md`](CLAUDE.md) — estado atual do projeto
- [`BLUEPRINT.md`](BLUEPRINT.md) — plano e decisões de arquitetura
- [`DEPLOY.md`](DEPLOY.md) — runbook de produção
- [`docs/`](docs/) — ADRs e guias internos