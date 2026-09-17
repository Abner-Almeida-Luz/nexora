# Documentação — NEXORA

Índice dos documentos de referência. Os documentos de identidade
(`README.md`, `CLAUDE.md`, `BLUEPRINT.md`, `DEPLOY.md`) ficam na raiz do
repositório; aqui ficam ADRs e guias.

## ADRs

Decisões de arquitetura, append-only. Cada decisão é imutável — para
reverter, crie um novo ADR referenciando o anterior.

Ver [`adr/README.md`](adr/README.md).

## Guias

Tutoriais internos de processos recorrentes ("como adicionar endpoint",
"como rodar migration", "como promover ADMIN").

Ver [`guides/README.md`](guides/README.md).

## Pendentes

Documentos que ainda não existem, mas serão criados quando a dor aparecer.
Não crie nada "para estar em dia" — crie quando precisar consultar.

- [ ] ADR sobre escolha de Railway + Vercel (por que não Render, Fly.io, etc.)
- [ ] ADR sobre refresh token sem rotação invalidante
- [ ] ADR sobre estrutura achatada de DTOs e mappers no backend
- [ ] Guia: como adicionar um novo endpoint (backend + frontend + teste)
- [ ] Guia: como promover usuário a ADMIN em produção
- [ ] Guia: como rodar uma migration nova