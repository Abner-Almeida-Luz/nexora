# ADRs — Architecture Decision Records

Registro das decisões arquiteturais do NEXORA.

## Formato

Cada ADR tem 4 seções curtas:

```markdown
# ADR-XXX — Título

**Contexto:** por que essa decisão foi necessária
**Decisão:** o que foi decidido
**Consequências:** o que isso custa e o que resolve
**Status:** proposto | aceito | substituído por ADR-YYY
```

## Regras

- **Append-only.** Nunca edite um ADR depois de aceito.
- Para reverter uma decisão, crie um novo ADR com "substitui ADR-XXX".
- Máximo 15 linhas por ADR. Se precisar de mais, vira guia.
- Só registre decisões que você não quer rever toda semana.

## Índice

*(nenhum ADR registrado ainda)*

Próximos candidatos (ver [`../README.md`](../README.md)):

- Escolha de Railway + Vercel
- Refresh token sem rotação
- Estrutura achatada de DTOs e mappers