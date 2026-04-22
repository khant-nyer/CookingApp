# 🍳 CookingApp API

A Spring Boot backend for managing foods, ingredients, recipes, and user accounts.

This repository powers the CookingApp REST API and is designed to be consumed by a web or mobile frontend.

## ✨ Highlights

- REST API for **Foods**, **Ingredients**, and **Recipes**
- **AWS Cognito** JWT authentication support
- User registration with **idempotency** protection
- Pageable listing endpoints
- OpenAPI docs + Swagger UI
- PostgreSQL + JPA/Hibernate persistence

## 🧱 Built With

- Java 21
- Spring Boot 4
- Spring Web / Data JPA / Validation / Security
- Spring OAuth2 Resource Server
- PostgreSQL
- AWS SDK (Cognito Identity Provider)
- springdoc-openapi (Swagger UI)
- Maven

## 📚 API Docs

When the app is running locally:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI spec: `http://localhost:8080/v3/api-docs`

## 🔐 Auth Model

- JWT bearer tokens are validated using Cognito configuration.
- Public endpoints include:
  - `POST /api/auth/register`
  - `GET /api/foods`
  - `GET /api/ingredients`
  - `GET /api/recipes`
  - Swagger/OpenAPI endpoints

Most mutating and profile endpoints require authentication.

## 🚀 Quick Start

### Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL

### Run locally

```bash
mvn spring-boot:run
```

### Run tests

```bash
mvn test
```

## ⚙️ Environment Variables

Common configuration keys:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SECURITY_COGNITO_REGION`
- `SECURITY_COGNITO_USER_POOL_ID`
- `SECURITY_COGNITO_APP_CLIENT_ID`
- `SECURITY_COGNITO_APP_CLIENT_IDS`
- `SECURITY_COGNITO_JWK_SET_URI`
- `APP_CORS_ALLOWED_ORIGINS`

The app also supports loading values from `.env` via Spring config import.

## 🐳 Docker

```bash
docker build -t cookingapp-api .
docker run --rm -p 8080:8080 cookingapp-api
```

## 📁 Repository Structure

```text
src/main/java/com/chef/william
├── config
├── controller
├── dto
├── exception
├── model
├── repository
└── service
```
