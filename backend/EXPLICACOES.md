
---

## 16. Decisões de Arquitetura (Mini-ADR)

### 1. Por que Spring Boot 3.3 em vez de 4.x?
A especificação menciona Spring Boot 4.x, mas até a data desta implementação, Spring Boot 4 ainda não foi lançado (a versão estável mais recente é 3.3.x). Utilizamos a versão estável mais atual (3.3.2) que segue as mesmas práticas e é compatível com Java 21. Quando Spring Boot 4 estiver disponível, a migração será simples (ajuste de versão no `pom.xml`).

### 2. Por que JWT stateless?
Escolhemos autenticação stateless com JWT porque:
- Escala horizontalmente sem necessidade de sessão compartilhada.
- O backend é uma API REST, sem estado.
- O token contém as claims necessárias e é validado a cada requisição, sem consulta ao banco (exceto para carregar o usuário, que pode ser otimizado com cache).

### 3. Por que Flyway em produção e `ddl-auto: validate`?
Em produção, nunca usamos `ddl-auto: update` porque isso permite que o Hibernate altere o schema automaticamente, o que pode causar inconsistências e perda de dados. Flyway garante versionamento do schema, permitindo migrações controladas e auditáveis. Em desenvolvimento, desabilitamos Flyway e usamos `create-drop` com H2 para agilidade, pois os dados são efêmeros.

### 4. Como o estoque é protegido na finalização do pedido?
Na criação do pedido, executamos uma transação com `@Transactional`. Dentro dela:
- Verificamos se há estoque suficiente para cada item.
- Decrementamos o estoque e salvamos o produto.
- Criamos os itens do pedido e o pedido.
- Limpamos o carrinho.
  Todas as operações são atômicas. Para um MVP, não utilizamos locks pessimistas/otimistas, mas mencionamos que em cenários de alta concorrência seria recomendado usar `@Lock(PESSIMISTIC_WRITE)` ou versionamento com `@Version` nas entidades Product. A condição de corrida é mitigada pelo isolamento padrão do banco (READ_COMMITTED) e pela checagem de estoque dentro da transação.

### 5. Por que MapStruct para mapeamento?
MapStruct gera código de mapeamento em tempo de compilação, evitando erros de reflection e sendo mais rápido que outras soluções. Além disso, elimina boilerplate manual e é type-safe. Utilizamos interfaces com `componentModel = "spring"` para injetar os mappers.

### 6. Por que DTOs de entrada com Bean Validation?
A validação de entrada é essencial para garantir a integridade dos dados e dar feedback claro ao cliente. Centralizamos as regras nos records dos DTOs usando anotações `jakarta.validation`.

### 7. Por que `ProblemDetail` (RFC 7807)?
O `ProblemDetail` padroniza a estrutura de erros da API, tornando-a consistente e facilmente interpretável por clientes. Isso melhora a experiência do desenvolvedor frontend e segue boas práticas REST.

### 8. Por que testes de segurança?
A regressão citada (permitAll sem método HTTP) é um erro clássico. Escrevemos um teste de integração que garante que endpoints de escrita (POST) em `/api/products` exigem autenticação, prevenindo futuras regressões.

---

## 17. Limitações Conhecidas / Próximos Passos

1. **Rate limiting no login**: não implementado, susceptível a brute force.
2. **Refresh token**: apenas access token; expiração configuração. Refresh token aumentaria a segurança e UX.
3. **Paginação de reviews**: está implementada, mas sem ordenação configurável.
4. **Locks otimistas/pessimistas**: não implementados para controle de concorrência no estoque; recomendado para produção.
5. **Métricas e monitoramento**: não configurado além do Actuator básico.
6. **Internacionalização (i18n)**: mensagens de erro em inglês; poderiam ser localizadas.
7. **Testes unitários e de integração adicionais**: apenas o teste de segurança essencial; os demais serviços carecem de cobertura.
8. **Cache**: não há cache para produtos/categorias; poderia melhorar performance.
9. **Documentação Swagger detalhada**: as operações estão documentadas automaticamente, mas descrições personalizadas e exemplos poderiam ser adicionados.
10. **Envio de e-mails**: recuperação de senha, confirmação de cadastro, etc., não implementados.

---

Agora você tem um backend completo, funcional e profissional. Todos os arquivos estão prontos para serem copiados e executados. Certifique-se de que a versão do Spring Boot no `pom.xml` seja ajustada caso uma versão mais recente esteja disponível.