# KeepCoding

Plataforma de desafios de programação (estilo LeetCode/HackerRank) com o
**DevCoach** — uma IA inspirada no Coach do Chess.com que classifica a
qualidade da solução (`Brilhante`, `Ótimo`, `Livro`, `Incompleto`, `Gafe`),
dá uma dica socrática e calcula a complexidade Big O.

## Stack

| Camada      | Tecnologia                                  |
|-------------|---------------------------------------------|
| Backend     | Java 21 · Spring Boot 3.4 · Spring AI 1.0   |
| Frontend    | Angular 19 (standalone + signals)           |
| Banco       | PostgreSQL 16                               |
| Sandbox     | Judge0 (integração preparada — hoje mock)   |
| IA          | Spring AI (provider OpenAI, trocável)       |

## Estrutura de pastas

```
keepcoding/
├── docker-compose.yml          # PostgreSQL local
├── docs/
│   └── schema.sql              # schema do banco (referência)
│
├── backend/                    # API Spring Boot
│   ├── pom.xml
│   └── src/main/
│       ├── resources/application.yml
│       └── java/com/keepcoding/
│           ├── KeepCodingApplication.java
│           ├── config/         # CORS, seed de dados
│           ├── controller/     # endpoints REST + tratamento de erro
│           ├── domain/         # entidades JPA + enums
│           ├── dto/            # records de request/response
│           ├── repository/     # repositórios Spring Data
│           └── service/        # SubmissionService, Judge0Service, CoachAiService
│
└── frontend/                   # SPA Angular
    ├── package.json
    ├── angular.json
    └── src/
        ├── environments/       # apiUrl
        └── app/
            ├── models/         # interfaces (Problem, Submission, Coach)
            ├── services/       # ProblemService, SubmissionService
            └── features/problem-detail/
                ├── problem-detail.component.*       # tela do desafio
                └── components/
                    ├── code-editor/                # editor (mock Monaco)
                    └── dev-coach/                  # avatar + balão + badge
```

## Como rodar

### 1. Banco de dados

```bash
docker compose up -d
```

### 2. Backend (porta 8080)

```bash
cd backend
# opcional: chave para o DevCoach real
export OPENAI_API_KEY=sk-...        # Windows PowerShell: $env:OPENAI_API_KEY="sk-..."
./mvnw spring-boot:run              # ou: mvn spring-boot:run
```

> Sem `OPENAI_API_KEY` a aplicação roda normalmente — o DevCoach devolve um
> feedback de *fallback*. O `DataSeeder` cria o usuário `demo` (id 1) e o
> problema `Two Sum` (id 1) na primeira execução.

### 3. Frontend (porta 4200)

```bash
cd frontend
npm install
npm start
```

Abra <http://localhost:4200>.

## Fluxo de submissão

```
Angular ProblemDetail → POST /api/submissions
   → SubmissionService  (salva PENDING)
   → Judge0Service      (executa no sandbox — mock)
   → CoachAiService     (Spring AI classifica a solução)
   → resposta com veredito + CoachFeedback → DevCoach component
```

## Próximos passos

- **Judge0 real**: substituir o corpo de `Judge0Service.execute` (esqueleto de
  `RestClient` já documentado no arquivo).
- **Monaco real**: `npm i ngx-monaco-editor-v2 monaco-editor` e trocar o
  `<textarea>` em `code-editor.component.html` (instruções no próprio arquivo).
  O pacote `@monaco-editor/angular` do enunciado é do ecossistema React; o
  equivalente Angular é `ngx-monaco-editor-v2`.
- **Auth**: hoje o `userId` é fixo (1). Adicionar Spring Security + JWT.
- **Migrations**: trocar `ddl-auto: update` por Flyway usando `docs/schema.sql`.
- **Endpoints de problemas**: expor `GET /api/problems` e ligar o
  `ProblemService` do frontend ao backend (hoje usa mock local).
