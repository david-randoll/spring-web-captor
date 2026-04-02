# Spring Web Captor Demo

An interactive demo showcasing the power of Spring Web Captor. Send HTTP requests through a React UI and see every captured field in real-time — headers, bodies, path params, query params, file uploads, durations, and more.

## Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 18+

## Setup

### 1. Install the library

From the repository root:

```bash
mvn install -DskipTests
```

### 2. Start the backend

```bash
cd examples/backend
mvn spring-boot:run
```

The backend starts on http://localhost:8085.

### 3. Start the frontend

```bash
cd examples/frontend
npm install
npm run dev
```

The frontend starts on http://localhost:5173.

## Docker

Run the entire demo with Docker Compose:

```bash
cd examples
docker compose up --build
```

- Frontend: http://localhost:3000
- Backend: http://localhost:8085

## Features Showcased

- **All HTTP methods** — GET, POST, PUT, PATCH, DELETE with JSON bodies
- **Path variables** — Including catch-all wildcard `{*rest}` patterns
- **Query parameters** — Multi-value parameter capturing
- **File uploads** — Multipart with base64-encoded file metadata
- **Form data** — URL-encoded form submissions
- **Plain text** — Text body parsing
- **Error capturing** — 400, 404, 500, 418 with full error details
- **Duration tracking** — Request timing via the duration extension
- **Concurrent requests** — Thread-safe parallel capture
- **IP & User-Agent** — Automatic client metadata extraction
