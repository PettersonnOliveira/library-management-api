# 📚 Library Management API

REST API developed to manage a library's catalog and loans. It allows registering users, adding books, creating loans, handling returns, and consulting movement history.

This is the second project in my Java and Spring Boot study portfolio, focusing on entity relationships, business rules, and professional error handling.

## 💡 Problem it solves

Libraries that manage loans manually (spreadsheets, notebooks, informal tracking) face issues such as:

- Not knowing which books are currently available
- Losing the history of who borrowed which book and when
- Lending a book that is already loaned out
- Recording duplicate returns by mistake

The Library Management API centralizes catalog and loan control, enforcing business rules to prevent invalid operations.

## 🛠️ Technologies used

- **Java 21**
- **Spring Boot**
- **Spring Web** (REST API)
- **Spring Data JPA**
- **PostgreSQL** (development/production) and **H2** (tests)
- **Spring Security + JWT** (authentication and authorization)
- **Bean Validation** (input validation)
- **Maven**
- **HTML/CSS/JavaScript** (frontend served as static files)

## 🏗️ Architecture

```
Controller → Service → Repository → Database
```

- **Controller**: receives HTTP requests and returns responses
- **Service**: contains business rules, including cross-entity validations
- **Repository**: data access to the database
- **DTOs**: protect the API by controlling exactly what goes in and out — including nested DTOs for relationships
- **Exceptions**: custom exceptions handled globally to ensure consistent error responses
- **Frontend**: a plain HTML/CSS/JavaScript SPA served by Spring Boot from `src/main/resources/static`

## 📦 Data model

### User
| Field | Type |
|---|---|
| id | Long |
| name | String |
| email | String (unique) |
| password | String (BCrypt hash) |
| role | `USER` or `ADMIN` |

### Book
| Field | Type |
|---|---|
| id | Long |
| title | String |
| author | String |
| available | boolean |

### Loan
| Field | Type |
|---|---|
| id | Long |
| loanDate | LocalDateTime |
| returnDate | LocalDateTime (null until returned) |
| user | User (relationship `@ManyToOne`) |
| book | Book (relationship `@ManyToOne`) |

## 🧠 Relationships

- A `User` can have many `Loan` records over time
- A `Book` can appear in many different `Loan` records over time
- Each `Loan` belongs to exactly 1 `User` and 1 `Book`

## 🔐 Authentication and authorization

The API is **stateless** and protected with **Spring Security + JWT**:

- Passwords are stored as **BCrypt** hashes (never in plain text)
- Login returns a JWT signed with HMAC (24h validity by default)
- Public routes: `POST /auth/register` and `POST /auth/login`
- Every other route requires the `Authorization: Bearer <token>` header
- `401 Unauthorized` for a missing/invalid token and `403 Forbidden` for missing permission

### Roles

| Role | Permissions |
|---|---|
| `USER` | Browse books, create loans, and view/return **only their own** loans |
| `ADMIN` | Everything a `USER` can do + create/update books, manage users, and view/return any loan |

### Authentication flow

**1. Register** — `POST /auth/register`

```json
{
  "name": "Maria Silva",
  "email": "maria@email.com",
  "password": "strong-password-123"
}
```

**2. Login** — `POST /auth/login`

```json
{
  "email": "maria@email.com",
  "password": "strong-password-123"
}
```

**Response (200 OK)**

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

**3. Using the token**

```http
GET /loans
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Creating the first ADMIN

Public registration always creates `USER` accounts, and admin routes require `ADMIN`. To avoid this deadlock, set the environment variables before starting the application:

```bash
ADMIN_EMAIL=admin@library.com
ADMIN_PASSWORD=a-strong-password
```

On startup, `AdminBootstrap` creates that user as `ADMIN` (or promotes the existing user with that e-mail). If the variables are not set, no admin is created.

## 📌 Available endpoints

### Users
| Action | Method | Route | Access |
|---|---|---|---|
| Create user | `POST` | `/users` | `ADMIN` |
| List users | `GET` | `/users` | `ADMIN` |
| Get user by ID | `GET` | `/users/{id}` | `ADMIN` |

### Books
| Action | Method | Route | Access |
|---|---|---|---|
| Create book | `POST` | `/books` | `ADMIN` |
| List books | `GET` | `/books` | Authenticated |
| Get book by ID | `GET` | `/books/{id}` | Authenticated |
| Update book | `PUT` | `/books/{id}` | `ADMIN` |

### Loans
| Action | Method | Route | Access |
|---|---|---|---|
| Create loan | `POST` | `/loans` | Authenticated (`USER` borrows for themselves; `ADMIN` provides `userId`) |
| List loans | `GET` | `/loans` | Authenticated (`USER` sees only their own) |
| Get loan by ID | `GET` | `/loans/{id}` | Owner or `ADMIN` |
| Return book | `PUT` | `/loans/{id}/return` | Owner or `ADMIN` |

### Example — Create loan

**POST** `/loans`

```json
{
  "userId": 1,
  "bookId": 1
}
```

**Response (201 Created)**

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

> Note that `book.available` is `false` — the Service marks the book as unavailable as soon as the loan is created.

## 🔥 Business rules

| Rule | Behavior |
|---|---|
| Unavailable book cannot be loaned | `400 Bad Request` |
| User must exist | `404 Not Found` |
| Book must exist | `404 Not Found` |
| Cannot return an already returned loan | `400 Bad Request` |
| Duplicate e-mail on registration | `409 Conflict` |
| Cannot change a book's availability while it has an active loan | `400 Bad Request` |

## ⚠️ Error handling

The API uses custom exceptions handled globally via `@RestControllerAdvice`, ensuring consistent error responses instead of generic server errors:

- **`ResourceNotFoundException`** → returns `404 Not Found` (used when a resource by ID is not found)
- **`BusinessRuleException`** → returns `400 Bad Request` (used when a business rule is violated even though the resource exists)
- **`ConflictException`** → returns `409 Conflict` (used when the resource already exists, such as a duplicate e-mail)
- **`AccessDeniedException`** → returns `403 Forbidden` (used when a `USER` tries to access someone else's loan)

## ✅ Validations

All input DTOs use Bean Validation:
- `name`, `email`, `title`, `author` → `@NotBlank` (cannot be null, empty or only whitespace)
- `userId`, `bookId` → `@NotNull` (required since they are numeric fields)
- `available` → optional on book update; when provided, it respects the active-loan rule

Requests that violate these rules return `400 Bad Request` automatically before reaching the Controller logic.

## ⚙️ How to run the project locally

### Prerequisites
- Java 21 installed
- PostgreSQL (local or cloud, e.g. Aiven)
- Maven (or use the included `mvnw`)

> Automated tests do not need PostgreSQL: they use H2 in memory.

### Steps

1. Clone the repository
```bash
git clone <repository-url>
```

2. Create the `.env` file from the example:
```bash
cp .env.example .env
```

3. Fill in the variables in `.env`:
```properties
DB_URL=jdbc:postgresql://localhost:5432/library_db
DB_USER=postgres
DB_PASSWORD=your-password
JWT_SECRET=a-random-secret-with-at-least-32-characters
ADMIN_EMAIL=admin@library.com
ADMIN_PASSWORD=a-strong-password
```

`application.properties` imports this file automatically (`spring.config.import`) and **contains no credentials**.

> ⚠️ `.env` is in `.gitignore`. Never commit secrets.

4. Run the application:
```bash
./mvnw spring-boot:run
```

5. API and frontend will be available at `http://localhost:8080` (the UI is served at `/`).

### Running the tests

```bash
./mvnw test
```

The suite uses H2 in memory (`src/test/resources/application.properties`) and covers the main business rules: loans, returns, loan ownership, book availability, and duplicate e-mail.

### Manual testing

Use [Postman](https://www.postman.com/) or [Insomnia](https://insomnia.rest/) to test the endpoints.

## 🗺️ Roadmap

- [x] Authentication and authorization with Spring Security + JWT
- [x] Automated tests for the business rules
- [ ] Endpoint to list loans for a specific user (`@OneToMany` on `User` entity)
- [ ] Return deadline and late fee calculation
- [ ] Pagination for listing endpoints
- [ ] Standardize error responses with `ProblemDetail`

## 🚀 Deploy (Render + Aiven PostgreSQL)

The application is ready to be deployed on [Render](https://render.com/) connected to an Aiven PostgreSQL database.

### Environment variables (Render)
| Variable | Description |
|---|---|
| `DB_URL` | `jdbc:postgresql://<aiven-host>:<port>/library_db?sslmode=require&serverTimezone=UTC` |
| `DB_USER` | Aiven user (e.g. `avnadmin`) |
| `DB_PASSWORD` | Aiven password |
| `JWT_SECRET` | long random secret (required, at least 32 characters) |
| `ADMIN_EMAIL` | initial ADMIN user e-mail |
| `ADMIN_PASSWORD` | initial ADMIN user password |
| `CORS_ALLOWED_ORIGINS` | allowed origins, comma-separated (e.g. `https://your-frontend.pages.dev`) |
| `PORT` | set automatically by Render |

### Health Check
Set the service health check path in Render to `/api/health` (returns `200 OK`).

### CORS
Allowed origins are configurable through the `CORS_ALLOWED_ORIGINS` variable (comma-separated). By default, only local development origins are allowed.

### Security note
No credentials are committed: `application.properties` only references environment variables. On Render, set `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (at least 32 characters) and, if you want the initial admin, `ADMIN_EMAIL`/`ADMIN_PASSWORD`. If any password has already been exposed, rotate it in Aiven.

## 📝 What I learned building this project

- Modeling `@ManyToOne` relationships with JPA
- Nested/composed DTOs to protect related data without exposing Entities
- Business rules depending on the state of multiple related entities
- Side effects between related entities (loan affects book availability)
- Custom exceptions and global error handling with `@RestControllerAdvice`
- Distinguishing between "resource not found" and "business rule violated" errors
- Input validation with Bean Validation
- Stateless JWT authentication with a custom filter (`OncePerRequestFilter`)
- Role-based authorization with `@PreAuthorize` plus ownership checks in the Service layer
- BCrypt password hashing and protection against privilege escalation on registration
- Transactions (`@Transactional`) and pessimistic locking to prevent duplicate loans under concurrency
- Integration tests with Spring Boot and H2

---

Developed by [Petterson Oliveira] as part of my Java and Spring Boot study portfolio.
