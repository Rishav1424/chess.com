================================================================================
GRANDMASTER.IO — API DOCUMENTATION
================================================================================

BASE CONFIGURATION
--------------------------------------------------------------------------------
Base HTTP URL:      http://localhost:8080
Base WebSocket URL: ws://localhost:8080/ws
Authentication:     All endpoints (except Auth) require an HTTP Header:
Authorization: Bearer <your_jwt_token>


================================================================================
STANDARD RESPONSE ENVELOPE
================================================================================

All REST endpoints return one of two shapes:

SUCCESS RESPONSE
----------------
HTTP 200 / 201
{
"success": true,
"data":    <endpoint-specific object or array>,
"message": "Optional human-readable message"
}

ERROR RESPONSE
--------------
HTTP 400 / 401 / 403 / 404 / 409 / 500
{
"success": false,
"error":   "MACHINE_READABLE_ERROR_CODE",
"message": "Human-readable explanation"
}

STANDARD ERROR CODES
--------------------
AUTHENTICATION_ERROR       401  Bad or expired credentials
AUTHORIZATION_ERROR        403  Valid token but not allowed to access this resource
GAME_NOT_FOUND             404  No game with the given ID exists in Redis or PostgreSQL
INVALID_ACTION             400  Illegal game action (wrong turn, invalid move, etc.)
INVALID_REQUEST_ERROR      400  Request body failed bean validation
DATA_INTEGRITY_ERROR       409  Unique constraint violated (e.g. username taken)



1 REST API ENDPOINTS
================================================================================

1.1 REGISTER NEW USER
---------------------
    Method:        POST 
    Endpoint:      /api/auth/register
    Auth Required: No
    Payload:       JSON
    {
        "username": "player1",          // required, unique
        "email":    "p1@example.com",   // required, unique, valid email
        "password": "securepass"        // required, min 4 characters
    }
    Returns:       201 Created. Body contains ApiSuccessResponse<AuthResponse>:
    {
        "success": true,
        "data": { "token": "<jwt-string>" },
        "message": "Registration success"
    }
    409 Conflict if username or email is already taken.
    400 Bad Request if validation fails.

1.2 USER LOGIN
--------------
    Method:        POST
    Endpoint:      /api/auth/login
    Auth Required: No
    Payload:       JSON
    {
        "username": "player1",
        "password": "securepass"
    }
    Returns:       200 OK. Body contains ApiSuccessResponse<AuthResponse>:
    {
        "success": true,
        "data": { "token": "<jwt-string>" },
        "message": "Login Success"
    }
    401 Unauthorized on bad credentials.

1.3 GET LIVE GAME STATUS
------------------------
    Method:        GET
    Endpoint:      /api/game/{gameId}/status
    Auth Required: Yes
    Description:   Retrieves the current volatile state of an active game from Redis.
    Returns live clock values adjusted for elapsed time since the last move.
    Returns:       200 OK. Body contains ApiSuccessResponse<GameStatusResponse>:
    {
    "success": true,
    "data": {
    "fen":             "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
    "whitePlayerName": "player1",
    "blackPlayerName": "player2",
    "whiteTime":       "PT2M45S",
    "blackTime":       "PT3M",
    "moves":           ["e2e4"]
    },
    "message": null
    }
Notes:
- "fen" reflects the board position after all confirmed moves.
  - "whiteTime" / "blackTime" are ISO 8601 Duration strings.
  Parse as: PT3M = 3 minutes, PT2M45S = 2 min 45 sec.
  - "moves" contains UCI strings in chronological order.
  404 if the game does not exist (already ended or invalid ID).

1.4 GET GAME HISTORY
--------------------
    Method:        GET
    Endpoint:      /api/game/{gameId}/history
    Auth Required: Yes
    Description:   Retrieves a completed game's persistent record from PostgreSQL.
    This endpoint works for both ongoing and finished games.
    Returns:       200 OK. Body contains ApiSuccessResponse<GameEntityResponse>:
    {
        "success": true,
        "data": {
        "id":              1,
        "whitePlayerName": "player1",
        "blackPlayerName": "player2",
        "started":         "2025-06-11T10:00:00Z",
        "finished":        "2025-06-11T10:07:32Z",
        "status":          "WON_WHITE_CHECKMATE",
        "moves":           ["e2e4", "e7e5", "g1f3", "b8c6", "f1c4"]
    },
        "message": null
    }
        "status" values:
            ONGOING
            WON_WHITE_CHECKMATE | WON_BLACK_CHECKMATE
            WON_WHITE_TIMEOUT   | WON_BLACK_TIMEOUT
            WON_WHITE_RESIGNATION | WON_BLACK_RESIGNATION
            DRAW_STALEMATE | DRAW_AGREEMENT | DRAW_FIFTY_MOVE_RULE
            DRAW_THREEFOLD_REPETITION | DRAW_INSUFFICIENT_MATERIAL
    404 if the game does not exist.

1.5 GET USER STATISTICS
-----------------------
    Method:        GET
    Endpoint:      /api/users/me/stats
    Auth Required: Yes
    Description:   Returns win/draw/loss breakdown for the authenticated user.
    Returns:       200 OK. Body contains ApiSuccessResponse<UserStats>:
    {
        "success": true,
        "data": {
        "winAsWhite":  12,
        "winAsBlack":  9,
        "loseAsWhite": 3,
        "loseAsBlack": 5,
        "drawAsWhite": 2,
        "drawAsBlack": 1
    },
        "message": null
    }

1.6 GET USER'S RECENT GAMES
----------------------------
    Method:        GET
    Endpoint:      /api/users/me/games
    Auth Required: Yes
    Description:   Returns the last 10 games (by most recently updated) for the authenticated user, including the complete move list.
                    The "moves" array is intended for the game review feature.
    Returns:       200 OK. Body contains ApiSuccessResponse<List<GameEntityResponse>>:
    {
        "success": true,
        "data": [
            {
                "id":              42,
                "whitePlayerName": "player1",
                "blackPlayerName": "player2",
                "started":         "2025-06-11T10:00:00Z",
                "finished":        "2025-06-11T10:07:32Z",
                "status":          "WON_WHITE_CHECKMATE",
                "moves":           ["e2e4", "e7e5", ...]
            },
            ...
        ],
        "message": null
    }


2 WEBSOCKET (STOMP) DESTINATIONS
================================================================================

Connection
----------
Connect to /ws using the SockJS client. Pass the JWT in the STOMP CONNECT frame
native headers:

    Authorization: Bearer <your_jwt_token>

Connections with a missing, invalid, or expired token are rejected immediately.

Error Channel
-------------
All server-side WebSocket errors are routed to the user's personal error queue:

    Subscribe: /user/queue/errors
    Receives:  Plain text string describing the error.


2.1 CLIENT SENDS (Triggers Server Actions)
------------------------------------------

    Action:      Join Matchmaking
    Destination: /app/match-making/join
    Payload:     None. The server identifies the user via the STOMP Principal.
    
    Action:      Cancel Matchmaking
    Destination: /app/match-making/cancel
    Payload:     None.
    
    Action:      Submit a Move
    Destination: /app/game/{gameId}/move
    Payload:     Plain text string containing the move in UCI notation.
    Format:    <from-square><to-square>
    Examples:  "e2e4"   (pawn advance)
    "e1g1"   (kingside castling)
    "e7e8q"  (queen promotion — append piece character)
    
    Action:      Resign Game
    Destination: /app/game/{gameId}/action/resign
    Payload:     None.
    
    Action:      Offer or Accept Draw
    Destination: /app/game/{gameId}/action/draw
    Payload:     None.

First call from a player sends a draw offer to the opponent.
If the opponent calls this within 30 seconds, the game ends as a draw.
After 30 seconds the offer expires.


2.2 CLIENT SUBSCRIBES (Listens for Server Events)
-------------------------------------------------

    Event:       Match Found Notification
    Topic:       /user/queue/match-making
    Receives:    JSON match details.
    {
        "gameId":     123,
        "opponentId": "player2",
        "playerSide": "WHITE"   // or "BLACK"
    }

    Event:       Move Broadcast
    Topic:       /topic/game/{gameId}/move
    Receives:    Plain text string containing the confirmed move in UCI notation (e.g. "e2e4", "e1g1", "e7e8q").

Both players receive this broadcast. The client that submitted the
move should deduplicate it against their local game history.

    Event:       Game Over
    Topic:       /topic/game/{gameId}/event
    Receives:    Plain text string containing the GameStatus enum value.
    Examples: "WON_WHITE_CHECKMATE", "DRAW_AGREEMENT", "WON_BLACK_TIMEOUT", "WON_WHITE_RESIGNATION"

.

    Event:       Draw Offer Received
    Topic:       /user/queue/game/{gameId}/event
    Receives:    Plain text string indicating which side made the offer.
    "WHITE_DRAW_REQUEST"  — White has offered a draw.
    "BLACK_DRAW_REQUEST"  — Black has offered a draw.
    To accept, publish to /app/game/{gameId}/action/draw within 30 seconds.


3 GAME CLOCK SPECIFICATION
================================================================================

Starting time per player:  3 minutes (PT3M)
Increment per move:         5 seconds (added after each move is confirmed)
Clock format in API:        ISO 8601 Duration (e.g. PT2M45.321S)
Timeout detection:          Server-side sweeper polling every 1 second
Clock authority:            Server only — client-side timers are for display only

Time returned by GET /status is adjusted for elapsed time since the last move,
so the value is accurate at the moment of the HTTP response.



4 MOVE FORMAT REFERENCE
================================================================================

All moves use UCI (Universal Chess Interface) notation:

<from-square><to-square>[<promotion>]

Squares are lowercase algebraic: a1–h8.

Examples:
e2e4     Pawn from e2 to e4
d1h5     Queen from d1 to h5
e1g1     King castles kingside (white)
e1c1     King castles queenside (white)
e8g8     King castles kingside (black)
e7e8q    Pawn promotes to queen on e8
e7e8r    Pawn promotes to rook on e8
e7e8b    Pawn promotes to bishop on e8
e7e8n    Pawn promotes to knight on e8

Promotion piece characters: q (queen), r (rook), b (bishop), n (knight)


5 AUTHENTICATION NOTES
================================================================================

- Tokens are signed with HS256 and expire after 24 hours (86400000 ms default).
- Include the token in every HTTP request:
  Authorization: Bearer <token>
- Include the token in the STOMP CONNECT frame native headers (same format).
- On expiry, re-authenticate via POST /api/auth/login to obtain a new token.
- Registration (POST /api/auth/register) also returns a token, so a separate
  login step is not required immediately after creating an account.