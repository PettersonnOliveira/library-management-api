# 📚 Library Management API

API REST desenvolvida para gerenciar o acervo e os empréstimos de uma biblioteca. Permite cadastrar usuários, cadastrar livros, registrar empréstimos, controlar devoluções e consultar o histórico de movimentações.

Este é o segundo projeto do meu portfólio de estudos em Java e Spring Boot, com foco em relacionamentos entre entidades, regras de negócio e tratamento de erros de forma profissional.

## 💡 Problema que resolve

Bibliotecas que controlam empréstimos manualmente (planilhas, cadernos, controle informal) enfrentam problemas como:

- Não saber quais livros estão disponíveis no momento
- Perder o histórico de quem pegou qual livro e quando
- Emprestar um livro que já está emprestado
- Registrar devoluções duplicadas por engano

A Library Management API resolve isso centralizando o controle de acervo e empréstimos, com regras de negócio que impedem operações inválidas.

## 🛠️ Tecnologias utilizadas

- **Java 21**
- **Spring Boot**
- **Spring Web** (REST API)
- **Spring Data JPA**
- **PostgreSQL** (desenvolvimento/produção) e **H2** (testes)
- **Spring Security + JWT** (autenticação e autorização)
- **Bean Validation** (validação de dados de entrada)
- **Maven**
- **HTML/CSS/JavaScript** (frontend servido como arquivos estáticos)

## 🏗️ Arquitetura

```
Controller → Service → Repository → Banco de Dados
```

- **Controller**: recebe requisições HTTP e devolve respostas
- **Service**: contém as regras de negócio, incluindo validações entre entidades
- **Repository**: comunicação com o banco de dados
- **DTOs**: protegem a API, controlando exatamente o que entra e sai — inclusive em relacionamentos, através de DTOs aninhados
- **Exceptions**: exceções customizadas capturadas globalmente, garantindo respostas de erro consistentes
- **Frontend**: SPA em HTML/CSS/JavaScript puro, servida pelo próprio Spring Boot a partir de `src/main/resources/static`

## 📦 Modelo de dados

### User
| Campo | Tipo |
|---|---|
| id | Long |
| name | String |
| email | String (único) |
| password | String (hash BCrypt) |
| role | `USER` ou `ADMIN` |

### Book
| Campo | Tipo |
|---|---|
| id | Long |
| title | String |
| author | String |
| available | boolean |

### Loan
| Campo | Tipo |
|---|---|
| id | Long |
| loanDate | LocalDateTime |
| returnDate | LocalDateTime (nulo até a devolução) |
| user | User (relacionamento `@ManyToOne`) |
| book | Book (relacionamento `@ManyToOne`) |

## 🧠 Relacionamentos

- Um `User` pode ter vários `Loan` ao longo do tempo
- Um `Book` pode aparecer em vários `Loan` diferentes ao longo do tempo
- Cada `Loan` pertence a exatamente 1 `User` e 1 `Book`

## 🔐 Autenticação e autorização

A API é **stateless** e protegida com **Spring Security + JWT**:

- Senhas são armazenadas com hash **BCrypt** (nunca em texto puro)
- O login devolve um token JWT assinado com HMAC (validade padrão de 24h)
- Rotas públicas: `POST /auth/register` e `POST /auth/login`
- Todas as outras rotas exigem o header `Authorization: Bearer <token>`
- `401 Unauthorized` para token ausente/inválido e `403 Forbidden` para falta de permissão

### Perfis (roles)

| Role | Permissões |
|---|---|
| `USER` | Consultar livros, criar empréstimos e ver/devolver **apenas os próprios** empréstimos |
| `ADMIN` | Tudo do `USER` + criar/atualizar livros, gerenciar usuários e ver/devolver qualquer empréstimo |

### Fluxo de autenticação

**1. Cadastro** — `POST /auth/register`

```json
{
  "name": "Maria Silva",
  "email": "maria@email.com",
  "password": "senha-forte-123"
}
```

**2. Login** — `POST /auth/login`

```json
{
  "email": "maria@email.com",
  "password": "senha-forte-123"
}
```

**Resposta (200 OK)**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "name": "Maria Silva",
  "email": "maria@email.com",
  "role": "USER"
}
```

**3. Uso do token**

```http
GET /loans
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Criando o primeiro ADMIN

O cadastro público sempre cria usuários `USER`, e as rotas administrativas exigem `ADMIN`. Para evitar esse impasse, defina as variáveis de ambiente antes de subir a aplicação:

```bash
ADMIN_EMAIL=admin@library.com
ADMIN_PASSWORD=uma-senha-forte
```

Na inicialização, o `AdminBootstrap` cria esse usuário como `ADMIN` (ou promove o usuário existente com esse e-mail). Se as variáveis não forem definidas, nenhum admin é criado.

## 📌 Endpoints disponíveis

### Users
| Ação | Método | Rota | Acesso |
|---|---|---|---|
| Criar usuário | `POST` | `/users` | `ADMIN` |
| Listar usuários | `GET` | `/users` | `ADMIN` |
| Buscar usuário por ID | `GET` | `/users/{id}` | `ADMIN` |

### Books
| Ação | Método | Rota | Acesso |
|---|---|---|---|
| Criar livro | `POST` | `/books` | `ADMIN` |
| Listar livros | `GET` | `/books` | Autenticado |
| Buscar livro por ID | `GET` | `/books/{id}` | Autenticado |
| Atualizar livro | `PUT` | `/books/{id}` | `ADMIN` |

### Loans
| Ação | Método | Rota | Acesso |
|---|---|---|---|
| Emprestar livro | `POST` | `/loans` | Autenticado (`USER` empresta para si; `ADMIN` informa `userId`) |
| Listar empréstimos | `GET` | `/loans` | Autenticado (`USER` vê só os próprios) |
| Buscar empréstimo por ID | `GET` | `/loans/{id}` | Dono ou `ADMIN` |
| Devolver livro | `PUT` | `/loans/{id}/return` | Dono ou `ADMIN` |

### Exemplo — Criar empréstimo

**POST** `/loans`

```json
{
  "userId": 1,
  "bookId": 1
}
```

**Resposta (201 Created)**

```json
{
  "id": 1,
  "user": {
    "id": 1,
    "name": "Maria Silva",
    "email": "maria.silva@email.com"
  },
  "book": {
    "id": 1,
    "title": "Dom Casmurro",
    "author": "Machado de Assis",
    "available": false
  },
  "loanDate": "2026-07-08T16:18:13.127323",
  "returnDate": null
}
```

> Repare que o `book` aparece com `available: false` — o Service marca o livro como indisponível automaticamente assim que o empréstimo é criado.

## 🔥 Regras de negócio

| Regra | Comportamento |
|---|---|
| Livro indisponível não pode ser emprestado | `400 Bad Request` |
| Usuário precisa existir | `404 Not Found` |
| Livro precisa existir | `404 Not Found` |
| Não é possível devolver um empréstimo já devolvido | `400 Bad Request` |
| E-mail duplicado no cadastro | `409 Conflict` |
| Não é possível alterar a disponibilidade de um livro com empréstimo ativo | `400 Bad Request` |

## ⚠️ Tratamento de erros

A API utiliza exceções customizadas, capturadas globalmente através de `@RestControllerAdvice`, garantindo respostas de erro consistentes em vez de erros genéricos de servidor:

- **`ResourceNotFoundException`** → devolve `404 Not Found` (usado quando um recurso pelo ID não é encontrado)
- **`BusinessRuleException`** → devolve `400 Bad Request` (usado quando uma regra de negócio é violada, mesmo com o recurso existindo)
- **`ConflictException`** → devolve `409 Conflict` (usado quando o recurso já existe, como e-mail duplicado)
- **`AccessDeniedException`** → devolve `403 Forbidden` (usado quando um `USER` tenta acessar um empréstimo de outra pessoa)

## ✅ Validações

Todos os DTOs de entrada possuem validação via Bean Validation:
- `name`, `email`, `title`, `author` → `@NotBlank` (não podem ser nulos, vazios ou só espaços)
- `userId`, `bookId` → `@NotNull` (obrigatórios, por serem campos numéricos)
- `available` → opcional na atualização do livro; quando informado, respeita a regra de empréstimo ativo

Requisições que violam essas regras retornam `400 Bad Request` automaticamente, antes mesmo de chegar na lógica do Controller.

## ⚙️ Como rodar o projeto localmente

### Pré-requisitos
- Java 21 instalado
- PostgreSQL (local ou na nuvem, ex.: Aiven)
- Maven (ou usar o `mvnw` incluso no projeto)

> Os testes automatizados não precisam de PostgreSQL: usam H2 em memória.

### Passos

1. Clone o repositório
```bash
git clone <url-do-repositorio>
```

2. Crie o arquivo `.env` a partir do exemplo:
```bash
cp .env.example .env
```

3. Preencha as variáveis no `.env`:
```properties
DB_URL=jdbc:postgresql://localhost:5432/library_db
DB_USER=postgres
DB_PASSWORD=sua-senha
JWT_SECRET=um-segredo-aleatorio-com-pelo-menos-32-caracteres
ADMIN_EMAIL=admin@library.com
ADMIN_PASSWORD=uma-senha-forte
```

O `application.properties` importa esse arquivo automaticamente (`spring.config.import`) e **não contém credenciais**.

> ⚠️ O `.env` está no `.gitignore`. Nunca versione segredos.

4. Execute a aplicação:
```bash
./mvnw spring-boot:run
```

5. API e frontend estarão disponíveis em `http://localhost:8080` (a interface fica em `/`).

### Rodando os testes

```bash
./mvnw test
```

A suíte usa H2 em memória (`src/test/resources/application.properties`) e cobre as principais regras de negócio: empréstimo, devolução, propriedade do empréstimo, disponibilidade do livro e e-mail duplicado.

### Testando manualmente

Recomenda-se o uso do [Postman](https://www.postman.com/) ou [Insomnia](https://insomnia.rest/) para testar os endpoints.

## 🗺️ Próximos passos (roadmap)

- [x] Autenticação e autorização com Spring Security + JWT
- [x] Testes automatizados das regras de negócio
- [ ] Endpoint para listar empréstimos de um usuário específico (`@OneToMany` na entidade `User`)
- [ ] Prazo de devolução (deadline) e cálculo de atraso
- [ ] Paginação nas listagens
- [ ] Padronizar respostas de erro com `ProblemDetail`

## 🚀 Deploy (Render + Aiven PostgreSQL)

A aplicação está pronta para deploy no [Render](https://render.com/) conectada a um PostgreSQL do [Aiven](https://aiven.io/).

### Variáveis de ambiente (Render)
| Variável | Descrição |
|---|---|
| `DB_URL` | `jdbc:postgresql://<host-aiven>:<porta>/library_db?sslmode=require&serverTimezone=UTC` |
| `DB_USER` | usuário do Aiven (ex.: `avnadmin`) |
| `DB_PASSWORD` | senha do Aiven |
| `JWT_SECRET` | segredo longo e aleatório (obrigatório, mínimo de 32 caracteres) |
| `ADMIN_EMAIL` | e-mail do usuário ADMIN inicial |
| `ADMIN_PASSWORD` | senha do usuário ADMIN inicial |
| `CORS_ALLOWED_ORIGINS` | origens permitidas, separadas por vírgula (ex.: `https://seu-frontend.pages.dev`) |
| `PORT` | definida automaticamente pelo Render |

### Health Check
Configure o health check path do serviço no Render como `/api/health` (retorna `200 OK`).

### CORS
As origens permitidas são configuráveis pela variável `CORS_ALLOWED_ORIGINS` (separadas por vírgula). Por padrão, apenas origens locais de desenvolvimento são liberadas.

### Segurança
Nenhuma credencial fica versionada: o `application.properties` apenas referencia variáveis de ambiente. No Render, defina `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (mínimo de 32 caracteres) e, se quiser criar o admin inicial, `ADMIN_EMAIL`/`ADMIN_PASSWORD`. Se alguma senha já foi exposta, rotacione-a no Aiven.

## 📝 O que aprendi construindo esse projeto

- Modelagem de relacionamentos `@ManyToOne` entre entidades JPA
- DTOs aninhados/compostos para proteger dados em relacionamentos, sem expor Entities
- Regras de negócio que dependem do estado de múltiplas entidades ao mesmo tempo
- Efeitos colaterais entre entidades relacionadas (empréstimo afeta disponibilidade do livro)
- Exceções customizadas e tratamento global de erros com `@RestControllerAdvice`
- Separação entre erro de "recurso não encontrado" e "regra de negócio violada"
- Validação de dados de entrada com Bean Validation
- Autenticação stateless com JWT e filtro customizado (`OncePerRequestFilter`)
- Autorização por perfil com `@PreAuthorize` e checagem de propriedade no Service
- Hash de senhas com BCrypt e proteção contra escalonamento de privilégio no cadastro
- Transações (`@Transactional`) e lock pessimista para evitar empréstimos duplicados em concorrência
- Testes de integração com Spring Boot e H2

---

Desenvolvido por [Petterson Oliveira] como parte do meu portfólio de estudos em Java e Spring Boot.
