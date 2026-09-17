# NEXORA — Deploy

Runbook operacional de produção. Como subir, atualizar, diagnosticar e
recuperar. Complementa `CLAUDE.md` (estado) e `BLUEPRINT.md` (plano).

## URLs

| Serviço | URL |
|---|---|
| Frontend | https://nexora-hazel-zeta.vercel.app |
| Backend | https://nexora-production-1de3.up.railway.app |
| Swagger | https://nexora-production-1de3.up.railway.app/swagger-ui/index.html |
| Repositório | https://github.com/Abner-Almeida-Luz/nexora |
| Railway | https://railway.com/dashboard |
| Vercel | https://vercel.com/dashboard |

## Arquitetura

```
GitHub (monorepo)
├── backend/   ──► Railway (Root Directory: backend)
│                     └── Postgres (mesmo projeto Railway)
└── frontend/  ──► Vercel (Root Directory: frontend)
```

Railway e Vercel apontam para o mesmo repositório, mas cada um builda
apenas sua pasta. Push na `main` dispara deploy automático em ambos.

## Operação

**Conta ADMIN em produção:** `admin@diversao.com` (promovida via SQL em
2026-09-16).

**Versão do Postgres:** PostgreSQL 16 — mesma versão usada em dev local
e nos testes de integração via Testcontainers.

## Variáveis de ambiente

### Backend (Railway → serviço backend → Variables)

| Nome | Valor | Notas |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Ativa o perfil de produção |
| `JWT_SECRET` | Base64 de 256 bits | **Gerar novo — nunca reusar dev** |
| `JWT_EXPIRATION` | `86400000` | 24h |
| `JWT_REFRESH_EXPIRATION` | `2592000000` | 30 dias |
| `DB_URL` | `jdbc:postgresql://<host>:<port>/<db>` | Valores literais do card Postgres |
| `DB_USERNAME` | `<user>` | Idem |
| `DB_PASSWORD` | `<pass>` | Idem |
| `CORS_ALLOWED_ORIGINS` | `https://nexora-hazel-zeta.vercel.app` | URL do Vercel |

**Gerar `JWT_SECRET` novo:**

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

### Frontend (Vercel → Settings → Environment Variables)

| Nome | Valor |
|---|---|
| `VITE_API_BASE_URL` | `https://nexora-production-1de3.up.railway.app` |

**Importante:** variáveis do Vite são injetadas em **build time**. Alterar
`VITE_API_BASE_URL` no Vercel exige **redeploy** para fazer efeito.

## Como funciona o deploy automático

**Backend:** push na `main` → Railway detecta → rebuilda com Dockerfile →
redeploya → Flyway aplica migrations pendentes → app sobe.

**Frontend:** push na `main` → Vercel detecta → `npm run build` → publica
`dist/`. Preview deploy gerado para cada PR separadamente.

**CI:** GitHub Actions roda `mvn test` (backend) e `npm run build`
(frontend) em cada push. **Não é bloqueante** — se os testes falharem, o
deploy acontece mesmo assim. O gate real é o desenvolvedor não pushar
código quebrado.

## Smoke test após deploy

Após cada deploy, verificar nesta ordem:

1. `GET https://nexora-production-1de3.up.railway.app/v3/api-docs` → deve retornar JSON
2. `GET https://nexora-production-1de3.up.railway.app/api/products?page=0&size=5` → deve retornar uma página
3. Frontend carrega `/products` sem erro no console do navegador
4. Registrar um usuário de teste e fazer login
5. Adicionar um produto ao carrinho e finalizar um pedido
6. Verificar que o pedido aparece em `/profile`

Se algum passo falhar, ver a seção "Problemas comuns" antes de mexer em
configuração.

## Como promover um usuário a ADMIN

1. Registrar conta normalmente em `/register`
2. Railway → card do **Postgres** → aba **Data** ou **Query**
3. Executar:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'seu@email.com';
```

4. **Logout e login de novo** no frontend — o JWT antigo ainda carrega a
   role anterior no payload.

**Por que não automatizar:** seed em prod e endpoint de bootstrap são
vetores de ataque comuns. Promoção de ADMIN é operação consciente.

## Como rodar migration nova

1. Cria `backend/src/main/resources/db/migration/V3__descricao.sql`
2. Commit e push
3. Railway rebuilda, Flyway aplica automaticamente no boot
4. Verifica nos logs: `Successfully applied 1 migration`

**Nunca edite uma migration já aplicada.** O Flyway detecta checksum
mismatch e quebra o boot. Sempre crie uma nova.

## Como ver logs

**Backend:** Railway → serviço backend → Deployments → deploy mais recente → Logs

**Frontend:** Vercel → projeto → Deployment → Function/Static Logs

**Dica de leitura de log:** stacktraces Java são verbosos. Vá direto para
a **última** linha `Caused by:` — é onde está a causa raiz. A primeira
`Caused by:` normalmente é só o encadeamento de dependências Spring.

## Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| `blocked by CORS policy` no console | `CORS_ALLOWED_ORIGINS` errado/ausente | Verificar variável no Railway; redeploy |
| `jdbc:postgresql://:/` no log | `DB_URL` vazia ou referência não resolvida | Copiar valores literais do card Postgres |
| Página 404 ao recarregar rota | Falta `frontend/vercel.json` com rewrite | Criar o arquivo com rewrite para `/index.html` |
| Página em branco no Vercel | `VITE_API_BASE_URL` não definida no build | Adicionar env var, **redeploy** |
| Erro 500 no backend | Migração falhou ou variável faltando | Ver último `Caused by:` no log |
| Login sempre 401 | `JWT_SECRET` mudou entre deploys | Tokens antigos invalidam; usuário precisa logar de novo |

## Deploy do zero (recuperação)

Se algum dia precisar refazer tudo:

1. **Postgres no Railway**
   - New Project → Database → Add PostgreSQL
   - **Versão:** PostgreSQL 16 (mesma do blueprint)
   - Anotar `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`

2. **Backend no Railway**
   - New → Deploy from GitHub repo → escolher o monorepo
   - Settings → Root Directory = `backend`
   - Variables → adicionar todas as variáveis da seção acima
   - Settings → Networking → Generate Domain na porta **8080**
   - Flyway aplica V1 e V2 automaticamente

3. **Frontend no Vercel**
   - Add New → Project → importar o monorepo
   - Root Directory = `frontend`
   - Environment Variables → `VITE_API_BASE_URL`
   - Deploy

4. **Fechar o ciclo**
   - Railway → Variables → `CORS_ALLOWED_ORIGINS` = URL do Vercel
   - Aguardar redeploy automático do backend

5. **Promover ADMIN**
   - Registrar conta em `/register` no frontend
   - Rodar o SQL da seção "Como promover um usuário a ADMIN"

## Rotação de segredos

**`JWT_SECRET`:** trocar no Railway invalida **todos** os tokens ativos.
Usuários precisam logar de novo. Fazer em janela de manutenção.

**`DB_PASSWORD`:** editar pelo card do Postgres no Railway — a plataforma
propaga para o backend automaticamente se o backend referenciar as
variáveis do banco.

## Débitos operacionais

- `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` são valores literais, não
  referências `${{Postgres.*}}`. Se o banco for recriado, atualizar
  manualmente.
- CI (GitHub Actions) existe mas não bloqueia deploy.
- Sem monitoramento nem alertas. Erros só aparecem se alguém abrir os logs.
- Sem backup automatizado explícito — depende do Railway.