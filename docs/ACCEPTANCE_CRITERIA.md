# Acceptance Criteria

## Generate plan
- Given a valid event profile, the API returns schema-valid plan data.
- Budget total and category totals are computed server-side.
- If proposed costs exceed the total budget, return a clear warning and alternatives rather than claiming the plan fits.
- Estimated costs are visibly marked as estimates.
- On provider failure, existing event data remains unchanged and the UI offers retry.

## Revise plan
- The API returns a proposed change set before applying significant changes.
- Unrelated manual edits are preserved.
- The user can accept or reject the proposal.
- Accepted changes are persisted atomically and create a revision record.

## Authorization
- User A cannot read, modify, or delete User B's event by guessing an ID.
- No secret or password is returned in API responses or logs.

## UI
- Loading, empty, error, and success states exist.
- Forms show validation errors.
- Core workflows work on desktop and mobile widths.
- Keyboard focus is visible and color contrast is reasonable.
