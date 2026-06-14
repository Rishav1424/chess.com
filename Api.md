# Grandmaster.io — API Specification v1 (Target Design)

This document is the contract both backend and frontend should converge toward.
It supersedes `API.md`. Where it differs from the current implementation, the
"Migration Notes" section at the end explains what changes and why.

---

## 1. Conventions

| Aspect             | Rule                                                                        |
|--------------------|-----------------------------------------------------------------------------|
| Base REST URL      | `/api/v1`                                                                   |
| Base WebSocket URL | `/ws` (SockJS)                                                              |
| JSON field casing  | `camelCase`                                                                 |
| Timestamps         | ISO-8601 UTC, e.g. `2025-06-11T10:00:00Z`                                   |
| Durations          | ISO-8601 duration, e.g. `PT2M45S`                                           |
| Moves              | UCI notation, e.g. `e2e4`, `e1g1`, `e7e8q`                                  |
| Auth (REST)        | `Authorization: Bearer <jwt>` header                                        |
| Auth (WS)          | `Authorization: Bearer <jwt>` in the STOMP `CONNECT` frame's native headers |
| Resource naming    | Plural collections: `/games`, `/users`                                      |

---

## 2. Response Envelope

### Success

```json
{
  "success": true,
  "data": { }
}
```

No top-level `message` field on success — if a message is meaningful, it belongs
inside `data`. A bare `"Login Success"` string adds nothing a 200 status code
doesn't already say.

### Error

```json
{
  "success": false,
  "error": {
    "code": "GAME_NOT_FOUND",
    "message": "Game 42 does not exist.",
    "details": null
  }
}
```

`details` is `null` for all error types except validation errors, where it is an
array of field-level problems:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed.",
    "details": [
      { "field": "password", "message": "must be at least 4 characters" },
      { "field": "email", "message": "must be a valid email address" }
    ]
  }
}
```

This shape lets the frontend always do `if (!res.data.success) { showError(res.data.error.message) }`
and, for forms, additionally walk `error.details` to highlight individual fields —
without a second response shape for validation specifically.

---

## 3. Error Code Catalog

| Code                   | HTTP Status  | Meaning                                                                                                    |
|------------------------|--------------|------------------------------------------------------------------------------------------------------------|
| `VALIDATION_ERROR`     | 400          | Request body failed bean validation                                                                        |
| `INVALID_ACTION`       | 400          | Action not legal in the current game state (wrong turn, illegal move, game already over)                   |
| `AUTHENTICATION_ERROR` | 401          | Missing, malformed, or expired token; bad credentials                                                      |
| `AUTHORIZATION_ERROR`  | 403          | Authenticated, but not permitted to perform this action                                                    |
| `GAME_NOT_FOUND`       | 404          | No game record exists with this ID                                                                         |
| `USER_NOT_FOUND`       | 404          | No user exists with this ID/username                                                                       |
| `CONFLICT`             | 409          | Unique constraint violated (username/email taken)                                                          |
| `GAME_NOT_LIVE`        | 410          | Game existed but has ended — its live state has been cleared; fetch `GET /games/{id}` for the final record |
| `INTERNAL_ERROR`       | 500          | Unexpected server error                                                                                    |

`410 Gone` for `GAME_NOT_LIVE` is deliberate: it's a different condition from
`404 GAME_NOT_FOUND`. A spectator polling `/games/{id}/live` after the game ends
should get a clear "this is over, here's where to look instead" signal rather
than an ambiguous 404 that's indistinguishable from "this game never existed."

---

## 4. REST Endpoints

### 4.1 `POST /api/v1/auth/register`

**Auth:** None

**Request**
```json
{
  "username": "player1",
  "email": "player1@example.com",
  "password": "secure pass"
}
```
- `username`: required, unique, 3–50 chars
- `email`: required, unique, valid email
- `password`: required, min 8 chars (see Migration Notes — raised from 4)

**Response — `201 Created`**
```json
{
  "success": true,
  "data": {
    "token": "<jwt>",
    "tokenType": "Bearer",
    "expiresAt": "2025-06-12T10:00:00Z",
    "user": {
      "id": 17,
      "username": "player1",
      "email": "player1@example.com"
    }
  }
}
```

**Errors:** `VALIDATION_ERROR` (400), `CONFLICT` (409, username or email taken)

---

### 4.2 `POST /api/v1/auth/login`

**Auth:** None

**Request**
```json
{ "username": "player1", "password": "secure pass" }
```

**Response — `200 OK`** — same shape as register's `data`.

**Errors:** `VALIDATION_ERROR` (400), `AUTHENTICATION_ERROR` (401)

---

### 4.3 `GET /api/v1/users/me`

**Auth:** Required

**Response — `200 OK`**
```json
{
  "success": true,
  "data": {
    "id": 17,
    "username": "player1",
    "email": "player1@example.com"
  }
}
```

This endpoint exists so the frontend never needs to decode the JWT payload to
get the username — `AuthStore` should call this once after login/register and
cache the result, rather than running `JSON.parse(atob(token.split('.')[1]))`.

---

### 4.4 `GET /api/v1/users/me/stats`

**Auth:** Required

**Response — `200 OK`**
```json
{
  "success": true,
  "data": {
    "winAsWhite": 12,
    "winAsBlack": 9,
    "loseAsWhite": 3,
    "loseAsBlack": 5,
    "drawAsWhite": 2,
    "drawAsBlack": 1
  }
}
```

Unchanged from current implementation — the raw counts are correct as the API
contract; derived values (win rate, totals) are presentation logic and stay on
the frontend.

---

### 4.5 `GET /api/v1/users/me/games`

**Auth:** Required

**Query params**
| Param | Default | Max | Description |
|---|---|---|---|
| `limit` | 10 | 50 | Number of games to return |
| `offset` | 0 | — | Pagination offset |

**Response — `200 OK`**
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 42,
        "whitePlayerName": "player1",
        "blackPlayerName": "player2",
        "started": "2025-06-11T10:00:00Z",
        "finished": "2025-06-11T10:07:32Z",
        "status": "WON_WHITE_CHECKMATE",
        "moves": ["e2e4", "e7e5", "g1f3"]
      }
    ],
    "page": {
      "limit": 10,
      "offset": 0,
      "total": 37
    }
  }
}
```

`moves` stays on each list item — per your note, this array is consumed
directly by the review feature without a second request. If the list ever
needs to scale well beyond `limit=50` with very long games, consider dropping
`moves` from list items and requiring a follow-up `GET /games/{id}` for review
— but for the current scope, inlining it avoids an extra round trip and is the
right tradeoff.

---

### 4.6 `GET /api/v1/games/{gameId}`

**Auth:** Required (any authenticated user — spectator access is intentional, see Migration Notes)

**Description:** The durable game record from Postgres. Valid for both
ongoing and finished games.

**Response — `200 OK`**
```json
{
  "success": true,
  "data": {
    "id": 42,
    "whitePlayerName": "player1",
    "blackPlayerName": "player2",
    "started": "2025-06-11T10:00:00Z",
    "finished": null,
    "status": "ONGOING",
    "moves": ["e2e4", "e7e5", "g1f3"]
  }
}
```

For an ongoing game, `finished` is `null` and `moves` reflects progress so far.

**Errors:** `GAME_NOT_FOUND` (404)

---

### 4.7 `GET /api/v1/games/{gameId}/live`

**Auth:** Required (any authenticated user)

**Description:** Ephemeral live state from Redis — current FEN, clocks, and
any pending draw offer. Only valid while the game is in progress.

**Response — `200 OK`**
```json
{
  "success": true,
  "data": {
    "fen": "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
    "whitePlayerName": "player1",
    "blackPlayerName": "player2",
    "whiteTime": "PT2M45S",
    "blackTime": "PT3M",
    "moves": ["e2e4"],
    "pendingDrawOffer": null
  }
}
```

`pendingDrawOffer` is `"WHITE"`, `"BLACK"`, or `null` — it reflects whichever
side currently has an outstanding draw offer from the other side. This field
exists so a player who refreshes mid-offer can correctly restore the
"Accept/Decline" dialog instead of losing that state.

**Errors:**
- `GAME_NOT_FOUND` (404) — no such game ever existed
- `GAME_NOT_LIVE` (410) — game existed but has ended; client should call `GET /games/{id}` instead

---

## 5. WebSocket

### 5.1 Connection

Connect via SockJS to `/ws`. Pass the JWT in the STOMP `CONNECT` frame's native
headers:

```
Authorization: Bearer <jwt>
```

Connections with a missing, malformed, or expired token are rejected at
`CONNECT` time.

### 5.2 Client → Server Destinations

| Destination                        | Payload                               | Description                               |
|------------------------------------|---------------------------------------|-------------------------------------------|
| `/app/matchmaking/join`            | none                                  | Join the matchmaking pool                 |
| `/app/matchmaking/cancel`          | none                                  | Leave the matchmaking pool                |
| `/app/games/{gameId}/move`         | plain text UCI string (e.g. `"e2e4"`) | Submit a move                             |
| `/app/games/{gameId}/resign`       | none                                  | Resign the game                           |
| `/app/games/{gameId}/draw/offer`   | none                                  | Offer a draw to the opponent              |
| `/app/games/{gameId}/draw/accept`  | none                                  | Accept the opponent's pending draw offer  |
| `/app/games/{gameId}/draw/decline` | none                                  | Decline the opponent's pending draw offer |

**Move submission stays a plain UCI string.** The server needs nothing else
to process it, and request/response shapes are allowed to diverge — only the
*broadcast* (§5.3) carries the richer JSON envelope. This asymmetry is
intentional, not an inconsistency.

**Why three explicit draw actions instead of one stateful toggle:** a single
`/draw` endpoint that behaves as "offer" or "accept" depending on hidden
server-side state (whether `DRAW_PREFIX` is set) means the frontend cannot
know, before calling it, which action it's about to trigger — and cannot
render the correct button label without separately tracking that state itself.
Three named actions are self-documenting and let the frontend disable/enable
buttons based on `pendingDrawOffer` from `/games/{id}/live` or from the
`DRAW_OFFERED` event below.

---

### 5.3 Server → Client Destinations

| Destination                    | Payload                       | Description                            |
|--------------------------------|-------------------------------|----------------------------------------|
| `/user/queue/matchmaking`      | JSON                          | Match found notification               |
| `/topic/games/{gameId}/moves`  | JSON                          | Confirmed move broadcast               |
| `/topic/games/{gameId}/events` | JSON, discriminated by `type` | All other game lifecycle events        |
| `/user/queue/errors`           | plain text                    | Server-side error for the current user |

#### Match Found — `/user/queue/matchmaking`

```json
{
  "gameId": 123,
  "opponentId": "player2",
  "playerSide": "WHITE"
}
```

#### Move Broadcast — `/topic/games/{gameId}/moves`

```json
{
  "move": "e2e4",
  "fen": "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
  "timestamp": "2025-06-11T10:00:05.123Z"
}
```

- `move` — the UCI string just applied.
- `fen` — the resulting board state, as computed by the server. Clients can
  compare this against their own locally-applied chess.js state as a
  consistency check; a mismatch indicates client-side drift and should trigger
  a re-sync via `GET /games/{id}/live` rather than silently diverging.
- `timestamp` — server wall-clock time the move was processed, ISO-8601 UTC
  with millisecond precision. Lets spectators reconstruct an accurate timeline
  and gives a natural ordering key if move events and chat messages are ever
  merged into a single spectator feed.

Both players (and any spectators) receive this. The client that submitted the
move is responsible for deduplicating it against local history.

#### Game Events — `/topic/games/{gameId}/events`

A single topic carrying every non-move event, each tagged with a `type`
discriminator so one subscription replaces the three separate channels used
previously (move-event, draw-offer, and the would-be draw-expiry channel).
Every event also carries a `timestamp` (ISO-8601 UTC, millisecond precision)
for the same spectator-timeline and ordering reasons as the move broadcast.

**`GAME_OVER`** — the game has ended. Board becomes read-only.
```json
{ "type": "GAME_OVER", "status": "WON_WHITE_CHECKMATE", "timestamp": "2025-06-11T10:07:32.000Z" }
```
`status` is one of the `GameStatus` enum values (§6.1).

**`DRAW_OFFERED`** — one side has offered a draw. Sent to both players and
spectators; the offering side's own client should ignore it (it already
showed an optimistic "Draw offer sent" toast when it called `/draw/offer`).
```json
{ "type": "DRAW_OFFERED", "by": "WHITE", "timestamp": "2025-06-11T10:05:00.000Z" }
```

**`DRAW_DECLINED`** — the opponent declined the draw offer.
```json
{ "type": "DRAW_DECLINED", "by": "BLACK", "timestamp": "2025-06-11T10:05:12.000Z" }
```
`by` is the side that declined.

**`DRAW_EXPIRED`** — the 30-second draw offer window elapsed without a
response. Sent so the offering side's UI can clear its "Draw offer sent"
state and the opponent's UI can dismiss the offer dialog if still open.
```json
{ "type": "DRAW_EXPIRED", "timestamp": "2025-06-11T10:05:30.000Z" }
```

A single handler dispatches on `type`:

```ts
client.subscribe(`/topic/games/${gameId}/events`, (message) => {
  const event = JSON.parse(message.body);
  switch (event.type) {
    case "GAME_OVER":     return handleGameOver(event.status);
    case "DRAW_OFFERED":  return handleDrawOffered(event.by);
    case "DRAW_DECLINED": return handleDrawDeclined(event.by);
    case "DRAW_EXPIRED":  return handleDrawExpired();
  }
});
```

`WatchRoom` uses the same handler for `GAME_OVER` but treats `DRAW_OFFERED` /
`DRAW_DECLINED` / `DRAW_EXPIRED` as non-blocking toasts rather than dialogs —
spectators see informational banners, not interrupted game state.

**Forward-looking:** spectator-count and join/leave notifications fit this
same pattern as additional `type` values (e.g. `SPECTATOR_JOINED`,
`SPECTATOR_COUNT`) — no new topic required. Chat is the one feature that
warrants its own topic, `/topic/games/{gameId}/chat`, since it's
high-frequency and unrelated to game state:
```json
{ "sender": "player1", "message": "gg", "timestamp": "2025-06-11T10:07:40.000Z" }
```

#### Errors — `/user/queue/errors`

```
Not your turn
```

Plain text. Used for rejected actions (illegal move, acting on a game you're
not part of, etc.) that don't change game state and therefore don't need a
typed event.

---

## 6. Domain Reference

### 6.1 `GameStatus` Enum

| Value                        | Meaning                           |
|------------------------------|-----------------------------------|
| `ONGOING`                    | Game in progress                  |
| `ABANDONED`                  | Reserved for future use           |
| `DRAW_STALEMATE`             | Draw by stalemate                 |
| `DRAW_INSUFFICIENT_MATERIAL` | Draw — neither side can checkmate |
| `DRAW_FIFTY_MOVE_RULE`       | Draw by 50-move rule              |
| `DRAW_THREEFOLD_REPETITION`  | Draw by repetition                |
| `DRAW_AGREEMENT`             | Draw by mutual agreement          |
| `WON_WHITE_CHECKMATE`        | White wins by checkmate           |
| `WON_BLACK_CHECKMATE`        | Black wins by checkmate           |
| `WON_WHITE_TIMEOUT`          | White wins on time                |
| `WON_BLACK_TIMEOUT`          | Black wins on time                |
| `WON_WHITE_RESIGNATION`      | White wins by resignation         |
| `WON_BLACK_RESIGNATION`      | Black wins by resignation         |

### 6.2 Move Format (UCI)

```
<from-square><to-square>[<promotion>]
```

| Example   | Meaning                |
|-----------|------------------------|
| `e2e4`    | Pawn advance           |
| `e1g1`    | White kingside castle  |
| `e8c8`    | Black queenside castle |
| `e7e8q`   | Pawn promotes to queen |

Promotion characters: `q`, `r`, `b`, `n`.

### 6.3 Game Clock

| Parameter                | Value                  |
|--------------------------|------------------------|
| Starting time per player | 3 minutes (`PT3M`)     |
| Increment per move       | +5 seconds             |
| Timeout polling interval | 1 second (server-side) |
| Clock authority          | Server only            |

Values returned by `GET /games/{id}/live` are adjusted for elapsed time as of
the response — never negative (clamped to `PT0S`).

---

## 7. Migration Notes

A summary of every deviation from the current implementation and why.

| Change                                                                                                      | Rationale                                                                                                                                                                                                                                                                                                                                                                                                           |
|-------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `/api/game/...` → `/api/v1/games/...`                                                                       | Versioning for future breaking changes without disrupting existing clients; plural for REST collection convention                                                                                                                                                                                                                                                                                                   |
| `/status` → `/live`, `/history` folded into `GET /games/{id}`                                               | "Status" collided semantically with HTTP status; `/live` clearly signals ephemeral Redis-backed data with a distinct lifecycle (`GAME_NOT_LIVE` vs `GAME_NOT_FOUND`)                                                                                                                                                                                                                                                |
| Error shape: flat `error`/`message` → nested `error: {code, message, details}`                              | `details` gives validation errors a home without inventing a second response shape                                                                                                                                                                                                                                                                                                                                  |
| Success responses drop top-level `message`                                                                  | Redundant — `"Login Success"` with a 200 status says nothing the status code doesn't                                                                                                                                                                                                                                                                                                                                |
| `AuthResponse` gains `tokenType`, `expiresAt`, `user`                                                       | Removes the frontend's `atob(token.split('.')[1])` JWT-decoding hack entirely                                                                                                                                                                                                                                                                                                                                       |
| New `GET /users/me`                                                                                         | Single source of truth for current user's profile, used by `AuthStore` instead of decoding tokens                                                                                                                                                                                                                                                                                                                   |
| `/me/games` gains `limit`/`offset` + `page` metadata                                                        | Prevents the endpoint from becoming unbounded as users accumulate game history                                                                                                                                                                                                                                                                                                                                      |
| Single `/app/game/{id}/action` (string payload) → `/draw/offer`, `/draw/accept`, `/draw/decline`, `/resign` | Removes implicit state-dependent behavior; each endpoint is self-documenting and the frontend never has to guess what an action call will do                                                                                                                                                                                                                                                                        |
| `/event` + `/draw-offer` topics → single `/topic/games/{id}/events` with `type` discriminator               | One subscription instead of two; `type` field prevents payload-vocabulary collisions between terminal (`GAME_OVER`) and non-terminal (`DRAW_OFFERED`, etc.) events                                                                                                                                                                                                                                                  |
| New `DRAW_EXPIRED` event                                                                                    | Closes the gap where a stale draw-offer dialog could remain open past the 30-second TTL with no client-side signal                                                                                                                                                                                                                                                                                                  |
| `pendingDrawOffer` added to `/games/{id}/live`                                                              | Restores draw-offer UI state correctly after a page refresh                                                                                                                                                                                                                                                                                                                                                         |
| Move broadcast → JSON `{ move, fen, timestamp }`; move *submission* stays plain UCI string                  | Broadcast is the highest-traffic channel and the natural home for a server-authoritative `fen` (de-sync detection) and `timestamp` (spectator timeline ordering). JSON is additive — future fields (`clock`, `ply`) cost nothing to add later. Submission stays a string because the server needs nothing extra from the client; request/response shape divergence is normal and not a "JSON-everywhere" violation. |
| `timestamp` added to every `/events` payload                                                                | Consistent ordering key across `moves`, `events`, and the future `chat` topic — enables a unified spectator activity feed without per-topic special-casing                                                                                                                                                                                                                                                          |
| Password minimum raised to 8                                                                                | 4 characters is not a meaningful security floor; 8 is a low-friction baseline that's still trivial for users to satisfy                                                                                                                                                                                                                                                                                             |
| `GET /games/{id}` and `/live` open to any authenticated user                                                | Intentional — required for the Watch/spectator feature. Not an authorization gap.                                                                                                                                                                                                                                                                                                                                   |