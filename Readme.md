# Grandmaster.io — Distributed Online Chess Platform

A production-grade, real-time multiplayer chess platform built with a distributed Java backend and a modern React frontend. Designed around event-driven architecture, server-authoritative game state, and persistent session recovery.

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 21, Spring Boot 3.2 |
| **Real-time** | WebSocket, STOMP protocol, SockJS |
| **Auth** | JWT (stateless, HS256) |
| **Game State** | Redis (volatile, in-memory game state) |
| **Persistence** | PostgreSQL + Spring Data JPA |
| **Move Validation** | chesslib (bhlangonijr) |
| **Frontend** | React 19, TypeScript, Vite |
| **UI** | Tailwind CSS v4, shadcn/ui, Radix UI |
| **State Management** | Zustand |
| **Containerisation** | Docker, Docker Compose |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        React Frontend                           │
│   Zustand stores │ WebSocket/STOMP │ REST via Axios             │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP + WS
┌────────────────────────────▼────────────────────────────────────┐
│                     Spring Boot Monolith                        │
│                                                                 │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────────┐  │
│  │ REST Layer  │  │  WS/STOMP    │  │  Scheduled Tasks       │  │
│  │ /api/**     │  │  /app/**     │  │  Matchmaking (10s)     │  │
│  └──────┬──────┘  └──────┬───────┘  │  Timeout Sweep (1s)    │  │
│         │                │          └────────────────────────┘  │
│  ┌──────▼────────────────▼────────────────────────────────────┐ │
│  │                    Service Layer                           │ │
│  │     GameService │ MatchmakingService │ JwtService          │ │
│  └─────────────────────────┬──────────────────────────────────┘ │
│                            │                                    │
│         ┌──────────────────┼──────────────────┐                 │
│         │                  │                  │                 │
│  ┌──────▼──────┐  ┌────────▼───────┐  ┌───────▼───────┐         │
│  │    Redis    │  │  PostgreSQL    │  │  SimpMessaging│         │
│  │ Game State  │  │  Game History  │  │  Template     │         │
│  │ Move Lists  │  │  User Records  │  │  (WS Broker)  │         │
│  │ Matchmaking │  │  Statistics    │  └───────────────┘         │
│  │ Timeouts    │  └────────────────┘                            │
│  └─────────────┘                                                │
└─────────────────────────────────────────────────────────────────┘
```

### Why Two Databases?

**Redis** holds volatile, access-critical game state: current FEN, player timers, and move lists. Every move needs sub-millisecond read/write latency and Redis provides this without blocking other players' games. Data here is ephemeral by design — when a game ends, Redis entries are deleted.

**PostgreSQL** holds durable game records: completed game metadata, move history (for the review feature), and user accounts. It is the source of truth for anything that must survive a server restart.

---

## Key Technical Features

### Server-Authoritative Game Logic
All move validation runs on the server via chesslib. The client only sends UCI strings (`"e2e4"`); the server validates against the current FEN, updates state, and broadcasts the confirmed move to both players. Clients cannot spoof moves or clock values.

### Real-Time Clock Synchronisation
Each game tracks `whiteTime`, `blackTime`, and `lastMoveTime` in Redis. On every move, elapsed wall-clock time is deducted from the active player's clock and a configurable increment (`BONUS_PER_MOVE = 5s`) is added. A scheduled sweeper polls Redis every second to detect clock expiry.

### Matchmaking via Redis Sorted Set
Players join a matchmaking pool stored as a Redis Sorted Set (score = join timestamp). A scheduler runs every 10 seconds, pops pairs of players, creates a game, and notifies each player via STOMP user-destination queues.

### JWT WebSocket Authentication
The STOMP `CONNECT` frame carries the JWT in a native `Authorization` header. A custom `ChannelInterceptor` validates the token before the connection is established, setting the Spring `Principal` for the session. All subsequent frame routing uses this principal for user-targeted messaging.

### Structured Exception Handling
A `CustomErrorException` hierarchy maps domain errors to HTTP status codes and machine-readable error codes. A `@RestControllerAdvice` intercepts all controller exceptions and returns a consistent `ApiErrorResponse`. A separate `@ControllerAdvice` handles WebSocket message errors and routes them to the user's `/queue/errors`.

---

## Project Structure

```
src/main/java/app/chess/com/
├── config/
│   ├── RedisConfig.java          # Jackson-serialized RedisTemplates
│   ├── SecurityConfig.java       # JWT filter chain, CORS
│   └── WebSocketConfig.java      # STOMP broker, auth interceptor wiring
├── dto/                          # All request/response DTOs
│   ├── ApiSuccessResponse.java
│   ├── ApiErrorResponse.java
│   ├── AuthResponse.java
│   ├── GameEntityResponse.java
│   ├── GameStatusResponse.java
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   ├── MatchFoundNotification.java
│   └── UserStats.java
├── exception/                    # Custom exception hierarchy
│   ├── CustomErrorException.java
│   ├── GameNotFoundException.java
│   ├── InvalidActionException.java
│   ├── UnauthorizedGameAccessException.java
│   ├── GlobalRestExceptionHandler.java
│   └── GlobalSocketExceptionHandler.java
├── game/
│   ├── GameEntity.java           # JPA entity
│   ├── GameState.java            # Redis-serialized volatile state
│   ├── GameStatus.java           # Outcome enum
│   ├── GameRepository.java
│   ├── GameService.java          # Core game logic, clock, timeouts
│   ├── GameRequestController.java # REST: /api/game/{id}/status|history
│   └── GameSocketController.java  # WS: move, resign, draw actions
├── matchmaking/
│   ├── MatchMaker.java           # Matching strategy interface
│   ├── SerialMatchMaker.java     # FIFO implementation
│   ├── MatchmakingService.java   # Redis pool management
│   └── GameLobbyController.java  # WS: join/cancel matchmaking
├── security/
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   └── WebSocketInterceptor.java
└── user/
    ├── User.java                  # UserDetails entity
    ├── UserRepository.java
    ├── DatabaseUserDetailsService.java
    ├── AuthenticationController.java
    └── UserController.java
```

---

## Getting Started

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Node.js 20+ (for frontend)

### 1. Clone

```bash
git clone https://github.com/rishav1424/grandmaster-io.git
cd grandmaster-io
```

### 2. Configure environment

Create a `.env` file in the project root (never commit this):

```env
# PostgreSQL
POSTGRES_DB=chess_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_strong_password_here

# Redis
REDIS_PASSWORD=your_redis_password_here

# JWT — generate with: openssl rand -base64 64
JWT_SECRET=your_64_char_base64_secret_here

# JPA strategy: 'create' on first run, 'update' or 'validate' after
JPA_STRATEGY=update

# CORS — comma-separated allowed origins
CORS_ORIGINS=http://localhost:5173
```

A `.env.example` is committed to the repository with placeholder values.

### 3. Run backend (Docker Compose)

```bash
docker-compose up --build
```

This starts three containers: `chess-app` (Spring Boot on :8080), `db-service` (PostgreSQL on :5432), and `redis-service` (Redis on :6379).

### 4. Run frontend

```bash
cd client
npm install
npm run dev
```

The client starts at `http://localhost:5173` and proxies API calls to `http://localhost:8080`.

### 5. Environment variables reference

| Variable | Required | Default | Description |
|---|---|---|---|
| `SERVER_PORT` | No | `8080` | Spring Boot server port |
| `DB_URL` | Yes | — | JDBC URL for PostgreSQL |
| `DB_USER` | No | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | Yes | — | PostgreSQL password |
| `JPA_STRATEGY` | No | `update` | Hibernate DDL strategy |
| `JWT_SECRET` | Yes | — | Base64-encoded HS256 signing key (min 32 bytes) |
| `JWT_EXPIRATION` | No | `86400000` | Token TTL in milliseconds |
| `REDIS_HOST` | Yes | — | Redis hostname |
| `REDIS_PORT` | No | `6379` | Redis port |
| `REDIS_PASSWORD` | Yes | — | Redis auth password |
| `CORS_ORIGINS` | Yes | — | Comma-separated allowed CORS origins |

---

## API Reference

All REST endpoints follow a consistent response envelope:

**Success**
```json
{ "success": true, "data": { ... }, "message": "Optional message" }
```

**Error**
```json
{ "success": false, "error": "ERROR_CODE", "message": "Human-readable reason" }
```

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | No | Create account, returns JWT |
| `POST` | `/api/auth/login` | No | Authenticate, returns JWT |

### Game

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/game/{gameId}/status` | Yes | Live game state from Redis |
| `GET` | `/api/game/{gameId}/history` | Yes | Completed game record from PostgreSQL |

### User

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/users/me/stats` | Yes | Win/draw/loss statistics |
| `GET` | `/api/users/me/games` | Yes | Last 10 games with full move history |

Full request/response schemas are documented in [`API.md`](./API.md).

---

## WebSocket Reference

Connect to `ws://localhost:8080/ws` (SockJS fallback). Pass the JWT token in the STOMP `CONNECT` frame headers:

```
Authorization: Bearer <token>
```

### Client → Server

| Destination | Payload | Description |
|---|---|---|
| `/app/match-making/join` | none | Join matchmaking queue |
| `/app/match-making/cancel` | none | Leave matchmaking queue |
| `/app/game/{id}/move` | UCI string e.g. `"e2e4"` | Submit a move |
| `/app/game/{id}/action/resign` | none | Resign the game |
| `/app/game/{id}/action/draw` | none | Offer or accept a draw |

### Server → Client

| Topic | Type | Description |
|---|---|---|
| `/user/queue/match-making` | JSON | Match found notification |
| `/topic/game/{id}/move` | string (UCI) | Opponent's confirmed move |
| `/topic/game/{id}/event` | string (GameStatus) | Game over notification |
| `/user/queue/game/{id}/event` | string | Draw offer received (`WHITE_DRAW_REQUEST` / `BLACK_DRAW_REQUEST`) |
| `/user/queue/errors` | string | Server-side error message |

---

## Game Time Control

- **Starting time:** 3 minutes per player
- **Increment:** +5 seconds per move
- **Timeout detection:** Polling every 1 second via `@Scheduled` sweeper
- **Clock source:** Server wall clock (not client) — immune to client manipulation

---

## Roadmap

- [ ] Distributed lock per game (Redis `SET NX`) for multi-instance deployments
- [ ] Atomic timeout sweep via Lua script (eliminate TOCTOU race)
- [ ] ELO rating system
- [ ] Puzzle mode
- [ ] Spectator count display
- [ ] Game export (PGN format)

---

## License

MIT