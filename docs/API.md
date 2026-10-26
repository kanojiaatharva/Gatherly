# REST API Outline (v1)

Base path: `/api`

## Auth
- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/logout` (depending on chosen session/JWT design)
- `GET /auth/me`

## Events
- `GET /events`
- `POST /events`
- `GET /events/{eventId}`
- `PATCH /events/{eventId}`
- `DELETE /events/{eventId}`

## Planning
- `POST /events/{eventId}/plan/generate`
- `POST /events/{eventId}/plan/revise` — returns a proposed change set; do not silently overwrite user edits.
- `GET /events/{eventId}/revisions`

## Budget
- `GET /events/{eventId}/budget`
- `POST /events/{eventId}/budget-items`
- `PATCH /events/{eventId}/budget-items/{itemId}`
- `DELETE /events/{eventId}/budget-items/{itemId}`

## Tasks
- `GET /events/{eventId}/tasks`
- `POST /events/{eventId}/tasks`
- `PATCH /events/{eventId}/tasks/{taskId}`
- `DELETE /events/{eventId}/tasks/{taskId}`

## Vendors
- `GET /events/{eventId}/vendors`
- `POST /events/{eventId}/vendors`
- `PATCH /events/{eventId}/vendors/{vendorId}`
- `DELETE /events/{eventId}/vendors/{vendorId}`

## API conventions
- Validate request DTOs with Bean Validation.
- Return consistent error objects with a trace/request ID.
- Enforce ownership/authorization at service layer for every event-scoped request.
- Use pagination for collections as needed.
- Never trust client-provided owner IDs.
