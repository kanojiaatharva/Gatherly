# Product Requirements Document — Gatherly

## 1. Overview
Gatherly is a GenAI-powered event planning assistant that helps people plan weddings, birthdays, and community events. It transforms conversational requirements into an actionable plan and keeps the plan editable as constraints change.

## 2. Target users
- Individuals/families planning birthdays and weddings
- Community organizers and small event teams
- Users who need help balancing budget, schedule, and vendor tasks

## 3. MVP scope
### Must have
1. Create, edit, view, and delete an event.
2. Event setup: type, name, date, location, guest count, currency, total budget, style, dietary requirements, accessibility needs, and priorities.
3. Generate structured AI plan: budget categories, task checklist with due dates, personalized ideas, and vendor categories to research.
4. Editable budget with estimated vs actual cost and totals calculated by backend.
5. Timeline/checklist with status, priority, due date, and optional assignee.
6. Context-aware plan revision: user asks to change the plan; show a change preview before applying consequential changes.
7. Persistent data, input validation, user-owned event authorization, useful error states.
8. Clearly label AI costs as estimates unless backed by a verified quote.

### Later
- Vendor search and quote comparison from reliable sources
- Email/push reminders
- Calendar integration
- Collaborative planning and roles
- PDF export and invitation generation
- Multilingual voice input
- Payments or booking (only with explicit user confirmation and verified integrations)

## 4. Core user journey
1. User signs in.
2. Creates an event and enters key constraints.
3. AI asks only for missing essential information or generates a plan if sufficient information exists.
4. User reviews budget, timeline, tasks, and ideas.
5. User edits or asks the assistant to revise the plan.
6. Backend validates and saves changes; dashboard reflects updates.

## 5. Functional requirements
- Budget amounts must be non-negative and use decimal-safe arithmetic.
- Backend, not the LLM, computes totals and validates budget constraints.
- Task dates must be valid relative to the event date. If a task date is in the past, flag it and offer a practical alternative.
- Preserve manual edits when regenerating; present a diff/preview for bulk changes.
- Store plan version/change summary for significant AI revisions.
- Never claim a vendor is available, verified, or booked without supporting data.
- Users may export or delete their event data.
- Handle model timeout, invalid JSON, rate limits, and provider errors gracefully.

## 6. Non-functional requirements
- Responsive, accessible interface with keyboard support and visible focus states.
- Secrets only in environment variables/secret storage.
- Password hashing, authorization on every event-specific endpoint, validation, rate limiting, and safe logging.
- No sensitive user data in logs or model prompts unless necessary.
- Automated unit and integration tests.
- Database migrations for schema evolution.

## 7. Success metrics
- Time to first usable plan
- Successful structured-plan generation rate
- Budget validation failure rate
- Percentage of generated tasks edited by users
- Task completion rate
- User-reported usefulness
