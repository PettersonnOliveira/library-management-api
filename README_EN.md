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
- **MySQL**
- **Bean Validation** (input validation)
- **Maven**

## 🏗️ Architecture

```
Controller → Service → Repository → Database
```

- **Controller**: receives HTTP requests and returns responses
- **Service**: contains business rules, including cross-entity validations
- **Repository**: data access to the database
- **DTOs**: protect the API by controlling exactly what goes in and out — including nested DTOs for relationships
- **Exceptions**: custom exceptions handled globally to ensure consistent error responses

## 📦 Data model

### User
| Field | Type |
|---|---|
| id | Long |
| name | String |
| email | String |

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

## 📌 Available endpoints

### Users
| Action | Method | Route |
|---|---|---|
| Create user | `POST` | `/users` |
| List users | `GET` | `/users` |
| Get user by ID | `GET` | `/users/{id}` |

### Books
| Action | Method | Route |
|---|---|---|
| Create book | `POST` | `/books` |
| List books | `GET` | `/books` |
| Get book by ID | `GET` | `/books/{id}` |
| Update book | `PUT` | `/books/{id}` |

### Loans
| Action | Method | Route |
|---|---|---|
| Create loan | `POST` | `/loans` |
| List loans | `GET` | `/loans` |
| Get loan by ID | `GET` | `/loans/{id}` |
| Return book | `PUT` | `/loans/{id}/return` |

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

## ⚠️ Error handling

The API uses custom exceptions handled globally via `@RestControllerAdvice`, ensuring consistent error responses instead of generic server errors:

- **`ResourceNotFoundException`** → returns `404 Not Found` (used when a resource by ID is not found)
- **`BusinessRuleException`** → returns `400 Bad Request` (used when a business rule is violated even though the resource exists)

## ✅ Validations

All input DTOs use Bean Validation:
- `name`, `email`, `title`, `author` → `@NotBlank` (cannot be null, empty or only whitespace)
- `userId`, `bookId` → `@NotNull` (required since they are numeric fields)

Requests that violate these rules return `400 Bad Request` automatically before reaching the Controller logic.

## ⚙️ How to run the project locally

### Prerequisites
- Java 21 installed
- MySQL running locally
- Maven (or use the included `mvnw`)

### Steps

1. Clone the repository
```bash
git clone <repository-url>
```

2. Configure the database in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/library_db?createDatabaseIfNotExist=true
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
spring.jpa.hibernate.ddl-auto=update
```

> ⚠️ This file may contain sensitive credentials and should not contain real secrets in a production project.

3. Run the application:
```bash
./mvnw spring-boot:run
```

4. The API will be available at `http://localhost:8080`

### Testing

Use [Postman](https://www.postman.com/) or [Insomnia](https://insomnia.rest/) to test the endpoints.

## 🗺️ Roadmap

- [ ] Authentication and authorization with Spring Security + JWT
- [ ] Endpoint to list loans for a specific user (`@OneToMany` on `User` entity)
- [ ] Return deadline and late fee calculation
- [ ] Pagination for listing endpoints

## 📝 What I learned building this project

- Modeling `@ManyToOne` relationships with JPA
- Nested/composed DTOs to protect related data without exposing Entities
- Business rules depending on the state of multiple related entities
- Side effects between related entities (loan affects book availability)
- Custom exceptions and global error handling with `@RestControllerAdvice`
- Distinguishing between "resource not found" and "business rule violated" errors
- Input validation with Bean Validation

---

Developed by [Petterson Oliveira] as part of my Java and Spring Boot study portfolio.

