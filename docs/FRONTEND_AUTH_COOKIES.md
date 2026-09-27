# Contrato de autenticação web por cookies

Base: https://cointrol-backend.onrender.com/api/v1
Origem web permitida por padrão: https://financial-control-front-khaki.vercel.app

## Contrato HTTP

Todas as chamadas do navegador usam `withCredentials: true` (Angular) ou `credentials: "include"` (fetch), inclusive a obtenção do CSRF. Login, refresh e logout respondem **204, corpo vazio**. Não extrair tokens, enviar Authorization no cliente web ou armazenar credenciais no localStorage.

| Método e caminho | Entrada | Sucesso |
|---|---|---|
| GET /auth/csrf | Cookies enviados pelo navegador | 200: `{"token":"<csrf>","headerName":"X-CSRF-TOKEN"}` |
| POST /auth/login | JSON `{"email":"...","password":"..."}` + header CSRF | 204 + cookies de acesso, refresh e rotação da sessão CSRF |
| GET /users/me | Cookie de acesso | 200, contrato público de usuário existente |
| POST /auth/refresh | Cookie refresh + header CSRF; corpo dispensável | 204 + novos cookies; refresh anterior revogado |
| POST /auth/logout | Cookie refresh opcional + header CSRF; corpo dispensável | 204 + expiração dos dois cookies |

Respostas de autenticação têm `Cache-Control: no-store`, inclusive erros e CSRF. Nunca ler Set-Cookie via JavaScript: o navegador processa esses headers.

| Cookie | Path | Validade |
|---|---|---|
| access_token | /api/v1 | Tempo restante do JWT emitido |
| refresh_token | /api/v1/auth | Tempo restante do refresh persistido |
| COINTROL_CSRF_SESSION | /api/v1 | Cookie de sessão; estado no servidor expira por inatividade |

Todos têm **HttpOnly; Secure; SameSite=None**, sem Domain (host-only). Access e refresh são emitidos em headers Set-Cookie separados. Logout os substitui por valor vazio e Max-Age=0 nos mesmos caminhos e escopo.

## Fluxo CSRF e Angular

1. Antes de login, cadastro ou qualquer escrita, GET /auth/csrf com credenciais. Guarde apenas o token CSRF em memória.
2. Envie o token no header cujo nome veio na resposta em todos os POST/PUT/PATCH/DELETE, inclusive login, refresh e logout.
3. Após login ou refresh bem-sucedido, **aguarde um novo GET /auth/csrf antes de novas escritas**. O token anterior foi invalidado; o identificador da sessão também mudou.
4. GET /users/me e outras leituras não exigem header CSRF.
5. Logout revoga o refresh informado; token ausente, desconhecido ou já revogado também resulta em 204. A sessão anônima CSRF permanece utilizável, permitindo repetir o logout com o mesmo CSRF. Nenhuma autenticação é armazenada nela.
6. Um CSRF ausente, inválido, de outra sessão, expirado ou anterior ao login/refresh produz 403 `CSRF_INVALID` (antes de executar a mutação). Faça novo GET /auth/csrf e repita no máximo uma vez, preservando o mesmo payload e Idempotency-Key. Não repetir indiscriminadamente outros 403.
7. Sessões CSRF expiram após 30 minutos de inatividade por padrão. Reinício do servidor também as perde. Isso não revoga os refresh tokens: obtenha novo CSRF e renove a autenticação se necessário.
8. Serialize refreshes concorrentes e coordene abas; um refresh só pode ser usado uma vez. Se outra aba rotacionar CSRF, recupere pelo GET.

Exemplo Angular (o header CSRF precisa ser enviado explicitamente para a URL remota; não depender do interceptor XSRF padrão):

```typescript
const base = 'https://cointrol-backend.onrender.com/api/v1';
type Csrf = { token: string; headerName: string };

let csrf = await firstValueFrom(
  http.get<Csrf>(base + '/auth/csrf', { withCredentials: true })
);
const writeOptions = () => ({
  withCredentials: true,
  headers: { [csrf.headerName]: csrf.token }
});

await firstValueFrom(http.post<void>(
  base + '/auth/login', { email, password }, writeOptions()
)); // 204
csrf = await firstValueFrom(
  http.get<Csrf>(base + '/auth/csrf', { withCredentials: true })
);
const me = await firstValueFrom(
  http.get(base + '/users/me', { withCredentials: true })
);
await firstValueFrom(http.post<void>(base + '/auth/refresh', null, writeOptions()));
csrf = await firstValueFrom(
  http.get<Csrf>(base + '/auth/csrf', { withCredentials: true })
);
await firstValueFrom(http.post<void>(base + '/auth/logout', null, writeOptions()));
```

`http` é HttpClient; `firstValueFrom` vem de rxjs. Um interceptor próprio pode adicionar o CSRF apenas às mutações da API Cointrol. Não enviar esse token a serviços terceiros.

## Erros e compatibilidade

- 400: JSON/campos inválidos.
- 401: login/refresh inválido, ausente ou expirado; endpoint protegido sem access válido. Sem redirects HTML.
- 403 CSRF_INVALID: mutação pública ou autenticada sem CSRF válido. Em endpoint protegido sem autenticação, 401 tem precedência.
- 403 ACCESS_DENIED: autorização negada; obter CSRF não concede permissão.
- 429: rate limit/bloqueio de login, com Retry-After; regras anteriores preservadas.

O repositório contém exemplos curl/Swagger Bearer; não é possível inventariar consumidores externos por este checkout. O backend mantém aceitação de `Authorization: Bearer` nos endpoints protegidos. **Header Authorization presente tem precedência sobre access_token**; header inválido não faz fallback para cookie válido. Bearer também exige sessão/header CSRF nas mutações. Não existe mais endpoint de emissão de tokens em JSON. Os endpoints /auth usam suas credenciais próprias e ignoram access token, permitindo refresh/logout com access expirado.

Logout revoga o refresh e remove os cookies do navegador. Um JWT copiado anteriormente continua válido até sua expiração, preservando a semântica anterior; não foi introduzida blacklist de access tokens.

## Configuração

| Variável | Padrão / finalidade |
|---|---|
| CORS_ALLOWED_ORIGINS | `https://financial-control-front-khaki.vercel.app` fora do perfil local; lista de origens exatas separadas por vírgula |
| CSRF_SESSION_TIMEOUT | `30m`; timeout de inatividade da sessão CSRF |
| JWT_ACCESS_TOKEN_EXPIRATION_MINUTES | `15`; 1 a 60, também governa Max-Age de access |
| JWT_REFRESH_TOKEN_EXPIRATION_DAYS | `30`; 1 a 90, também governa Max-Age de refresh |
| JWT_SECRET, JWT_ACTIVE_KEY_ID, JWT_PREVIOUS_KEYS, JWT_ISSUER, JWT_AUDIENCE | Configuração e rotação existentes; ver SEGURANCA.md |
| SPRING_PROFILES_ACTIVE | `local` somente no desenvolvimento; produção não deve ativá-lo |

CORS permite GET/POST/PUT/PATCH/DELETE/OPTIONS; headers Content-Type, Idempotency-Key, X-CSRF-TOKEN e Authorization (compatibilidade). OPTIONS não exige autenticação ou CSRF. Credenciais habilitadas, nenhuma origem wildcard ou conjunto de previews Vercel. Configurar a lista de produção exatamente com as origens desejadas; o override substitui o padrão. O perfil local preserva a lista local existente, configurável por CORS_ALLOWED_ORIGINS. Usar HTTPS no desenvolvimento para comportamento confiável de cookies Secure; os atributos de segurança não são relaxados por perfil.

CSRF usa `HttpSessionCsrfTokenRepository` e o handler XOR padrão do Spring Security (proteção BREACH). É token sincronizado com estado servidor, não double-submit. O contexto autenticado permanece STATELESS: possuir apenas a sessão CSRF nunca autentica. A sessão reside na instância; múltiplas réplicas precisam de armazenamento de sessão compartilhado ou afinidade antes de escalar. Nenhuma infraestrutura foi alterada nesta implementação.

Referência: [CSRF no Spring Security](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).

## Limite de cookies de terceiros

Vercel → Render é cross-site. **SameSite=None não contorna bloqueios de cookies de terceiros** do navegador, modo privado, extensão ou política corporativa. Isso pode bloquear tanto autenticação quanto a sessão CSRF, mesmo com CORS e withCredentials corretos.

Alternativas de implantação: proxy same-origin do frontend para /api/v1, preservando Set-Cookie e seus paths, ou domínios próprios same-site (por exemplo app.example.com e api.example.com com HTTPS). Atualizar a origem CORS se necessário. Não adicionar Domain aos cookies. Essas alternativas exigem configuração separada de infraestrutura e não foram implantadas.
