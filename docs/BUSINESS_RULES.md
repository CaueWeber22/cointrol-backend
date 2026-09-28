# Regras de negócio

Este documento descreve o comportamento de negócio esperado do backend Cointrol. Quando houver dúvida, confirme a implementação atual nos use cases em `src/main/java/com/fcproject/application/core` e nos contratos em [API.md](API.md).

## Produto

Cointrol é uma API de controle financeiro pessoal. Cada usuário autenticado gerencia seus próprios recursos financeiros: contas, categorias, lançamentos, transferências, saldos e resumos.

O sistema deve proteger três invariantes centrais:

- um usuário não acessa nem altera recursos de outro usuário;
- operações financeiras preservam histórico;
- saldos e resumos são derivados dos lançamentos, não de campos editáveis manualmente.

## Usuários e autenticação

- E-mails são normalizados antes de cadastro e login.
- E-mail cadastrado deve ser único.
- Senhas nunca são retornadas pela API.
- Login bem-sucedido gera access token JWT e refresh token opaco.
- Access e refresh tokens são entregues ao navegador apenas por cookies HttpOnly; o refresh é persistido somente como hash SHA-256.
- Login, refresh e logout retornam 204, sem tokens no JSON. Toda mutação exige CSRF válido; login/refresh invalidam o CSRF anterior.
- Refresh token válido é rotacionado a cada uso; o anterior é revogado.
- Logout revoga o refresh token por hash, apaga os cookies e é idempotente para tokens ausentes, desconhecidos ou já revogados, desde que o CSRF seja válido. Access JWT já copiado continua válido até expirar.
- Falhas de login alimentam proteção contra abuso sem revelar se o usuário existe.
- Depois do limite configurado de falhas na janela, o identificador fica temporariamente bloqueado.
- Eventos de autenticação relevantes devem gerar auditoria.

## Escopo por usuário

- Contas, categorias, lançamentos, transferências e resumos são sempre filtrados por `userId`.
- O `userId` vem da autenticação, não do payload da requisição.
- Buscar recurso inexistente ou pertencente a outro usuário deve resultar em resposta de não encontrado.
- Relações entre conta, categoria, lançamento e transferência não podem cruzar proprietários.

## Contas

- Uma conta pertence a exatamente um usuário.
- Conta possui nome, tipo, moeda, status, versão e timestamps.
- Tipos aceitos para novas contas: `CHECKING`, `SAVINGS`, `INVESTMENT`.
- Moeda deve ser um código ISO 4217 válido, normalizado para maiúsculas.
- Nome é obrigatório, normalizado por espaços e limitado a 100 caracteres.
- Nomes ativos devem ser únicos por usuário.
- Conta nasce `ACTIVE`.
- Conta arquivada não recebe novos lançamentos nem transferências.
- Arquivar conta é idempotente.
- Saldo inicial, quando informado, pode ser zero ou positivo, vira lançamento `OPENING_BALANCE` confirmado e persistido junto com a conta.
- Não existe campo de saldo editável diretamente.

## Categorias

- Uma categoria pertence a exatamente um usuário.
- Categoria possui nome, tipo, status, versão e timestamps.
- Tipos suportados: `INCOME` e `EXPENSE`.
- Nome é obrigatório, normalizado por espaços e limitado a 100 caracteres.
- Categoria nasce `ACTIVE`.
- Categoria arquivada permanece no histórico, mas não pode ser usada em novos lançamentos.
- Arquivar categoria é idempotente.
- Categoria de receita só pode classificar lançamento `INCOME`.
- Categoria de despesa só pode classificar lançamento `EXPENSE`.

## Lançamentos financeiros

- Lançamentos pertencem a um usuário e a uma conta.
- Lançamentos diretos exigem conta, categoria, tipo, valor, status, data efetiva e `Idempotency-Key`.
- Criação direta aceita apenas `INCOME` e `EXPENSE`.
- Tipos internos `OPENING_BALANCE`, `TRANSFER_IN` e `TRANSFER_OUT` não podem ser criados diretamente pelos endpoints de lançamentos.
- Valor deve ser positivo, ter no máximo quatro casas decimais e respeitar o limite de precisão suportado.
- Status gravável deve ser `PENDING` ou `CLEARED`.
- `CANCELED` é alcançado somente por operação de cancelamento.
- Descrição é opcional, normalizada por espaços e limitada a 255 caracteres.
- Idempotency key é obrigatória na criação, normalizada e limitada a 100 caracteres.
- Repetir criação com mesma chave e mesmo payload retorna o lançamento já persistido.
- Repetir criação com mesma chave e payload diferente gera `IDEMPOTENCY_CONFLICT`.
- Lançamento cancelado não pode ser alterado.
- Cancelar lançamento comum é idempotente.
- Lançamentos de transferência não podem ser alterados ou cancelados pelos endpoints de lançamentos.
- Ao mover lançamento para outra conta, a nova conta deve ser do mesmo usuário e mesma moeda da conta original.

## Sinal financeiro

- `INCOME`, `OPENING_BALANCE` e `TRANSFER_IN` somam ao saldo.
- `EXPENSE` e `TRANSFER_OUT` subtraem do saldo.
- O valor armazenado continua positivo; o sinal é derivado do tipo.

## Transferências

- Transferência pertence a um usuário e é representada por um grupo.
- Criação exige contas origem/destino, valor, data efetiva e `Idempotency-Key`.
- Conta origem e conta destino devem ser diferentes.
- Ambas as contas devem estar ativas.
- Ambas as contas devem pertencer ao usuário autenticado.
- Ambas as contas devem ter a mesma moeda.
- Transferência cria um grupo `COMPLETED`, uma perna `TRANSFER_OUT` na origem e uma perna `TRANSFER_IN` no destino.
- As duas pernas são `CLEARED`.
- Grupo e pernas devem ser persistidos atomicamente.
- Repetir transferência com mesma chave e mesmo payload retorna a transferência já persistida.
- Repetir transferência com mesma chave e payload diferente gera `IDEMPOTENCY_CONFLICT`.
- As pernas de transferência são imutáveis fora da operação de transferência.

## Cancelamento de transferência

- Cancelamento acontece pelo grupo da transferência.
- Motivo de cancelamento é obrigatório, normalizado por espaços e limitado a 255 caracteres.
- Cancelar transferência já cancelada retorna o estado atual e não duplica efeito.
- Cancelamento muda o grupo para `CANCELED` e registra motivo, instante de cancelamento e atualização.
- Cancelamento marca as duas pernas como `CANCELED` com o mesmo instante.
- Grupo e pernas devem ser atualizados atomicamente.

## Saldos

- Saldos são calculados a partir de lançamentos.
- Saldo confirmado considera lançamentos `CLEARED`.
- Saldo pendente considera lançamentos `PENDING`.
- Saldo projetado é a soma de confirmado e pendente.
- Lançamentos `CANCELED` não devem contribuir para saldos.
- Transferências e saldo inicial afetam saldos conforme o sinal derivado do tipo.

## Resumos

- Resumos exigem intervalo `from` e `to`.
- Data inicial não pode ser posterior à data final.
- Resumos consideram lançamentos confirmados no período.
- Totais são separados por moeda.
- Receita soma apenas `INCOME`.
- Despesa soma apenas `EXPENSE`.
- `OPENING_BALANCE`, `TRANSFER_IN` e `TRANSFER_OUT` afetam patrimônio, mas não entram como receita ou despesa do período.
- Resumo por categoria usa a categoria associada ao lançamento.
- Resumo mensal agrupa por mês e moeda.

## Erros de negócio

- Use códigos estáveis para o frontend reagir a erros conhecidos.
- Validação de payload deve retornar erro claro, sem detalhes internos.
- Conflitos de idempotência, nomes ativos duplicados e alterações inválidas devem ser tratados como conflito de negócio.
- Violações de propriedade não devem revelar dados de outro usuário.
