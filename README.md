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
- **MySQL**
- **Bean Validation** (validação de dados de entrada)
- **Maven**

## 🏗️ Arquitetura

```
Controller → Service → Repository → Banco de Dados
```

- **Controller**: recebe requisições HTTP e devolve respostas
- **Service**: contém as regras de negócio, incluindo validações entre entidades
- **Repository**: comunicação com o banco de dados
- **DTOs**: protegem a API, controlando exatamente o que entra e sai — inclusive em relacionamentos, através de DTOs aninhados
- **Exceptions**: exceções customizadas capturadas globalmente, garantindo respostas de erro consistentes

## 📦 Modelo de dados

### User
| Campo | Tipo |
|---|---|
| id | Long |
| name | String |
| email | String |

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

## 📌 Endpoints disponíveis

### Users
| Ação | Método | Rota |
|---|---|---|
| Criar usuário | `POST` | `/users` |
| Listar usuários | `GET` | `/users` |
| Buscar usuário por ID | `GET` | `/users/{id}` |

### Books
| Ação | Método | Rota |
|---|---|---|
| Criar livro | `POST` | `/books` |
| Listar livros | `GET` | `/books` |
| Buscar livro por ID | `GET` | `/books/{id}` |
| Atualizar livro | `PUT` | `/books/{id}` |

### Loans
| Ação | Método | Rota |
|---|---|---|
| Emprestar livro | `POST` | `/loans` |
| Listar empréstimos | `GET` | `/loans` |
| Buscar empréstimo por ID | `GET` | `/loans/{id}` |
| Devolver livro | `PUT` | `/loans/{id}/return` |

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

## ⚠️ Tratamento de erros

A API utiliza exceções customizadas, capturadas globalmente através de `@RestControllerAdvice`, garantindo respostas de erro consistentes em vez de erros genéricos de servidor:

- **`ResourceNotFoundException`** → devolve `404 Not Found` (usado quando um recurso pelo ID não é encontrado)
- **`BusinessRuleException`** → devolve `400 Bad Request` (usado quando uma regra de negócio é violada, mesmo com o recurso existindo)

## ✅ Validações

Todos os DTOs de entrada possuem validação via Bean Validation:
- `name`, `email`, `title`, `author` → `@NotBlank` (não podem ser nulos, vazios ou só espaços)
- `userId`, `bookId` → `@NotNull` (obrigatórios, por serem campos numéricos)

Requisições que violam essas regras retornam `400 Bad Request` automaticamente, antes mesmo de chegar na lógica do Controller.

## ⚙️ Como rodar o projeto localmente

### Pré-requisitos
- Java 21 instalado
- MySQL rodando localmente
- Maven (ou usar o `mvnw` incluso no projeto)

### Passos

1. Clone o repositório
```bash
git clone <url-do-repositorio>
```

2. Configure o banco de dados no arquivo `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/library_db?createDatabaseIfNotExist=true
spring.datasource.username=SEU_USUARIO
spring.datasource.password=SUA_SENHA
spring.jpa.hibernate.ddl-auto=update
```

> ⚠️ Este arquivo contém credenciais sensíveis e não deveria ser versionado com dados reais em um projeto de produção.

3. Execute a aplicação:
```bash
./mvnw spring-boot:run
```

4. A API estará disponível em `http://localhost:8080`

### Testando

Recomenda-se o uso do [Postman](https://www.postman.com/) ou [Insomnia](https://insomnia.rest/) para testar os endpoints.

## 🗺️ Próximos passos (roadmap)

- [ ] Autenticação e autorização com Spring Security + JWT
- [ ] Endpoint para listar empréstimos de um usuário específico (`@OneToMany` na entidade `User`)
- [ ] Prazo de devolução (deadline) e cálculo de atraso
- [ ] Paginação nas listagens

## 📝 O que aprendi construindo esse projeto

- Modelagem de relacionamentos `@ManyToOne` entre entidades JPA
- DTOs aninhados/compostos para proteger dados em relacionamentos, sem expor Entities
- Regras de negócio que dependem do estado de múltiplas entidades ao mesmo tempo
- Efeitos colaterais entre entidades relacionadas (empréstimo afeta disponibilidade do livro)
- Exceções customizadas e tratamento global de erros com `@RestControllerAdvice`
- Separação entre erro de "recurso não encontrado" e "regra de negócio violada"
- Validação de dados de entrada com Bean Validation

---

Desenvolvido por [Petterson Oliveira] como parte do meu portfólio de estudos em Java e Spring Boot.

