# OVS API Contracts

This file captures the current stable shape for public contracts used by the OVS demo.

## Response Envelope

```json
{
  "success": true,
  "message": "Human-readable status",
  "data": {},
  "timestamp": "2026-05-01T12:00:00Z"
}
```

## Implemented Demo Endpoints

- `GET /api/config`
- `GET /api/services`
- `GET /api/overview`
- `POST /api/auth/login`
- `GET /api/admin/voters`
- `POST /api/admin/voters`
- `PATCH /api/admin/voters/{voterId}`
- `GET /api/identity/roles`
- `POST /api/identity/login`
- `GET /api/identity/members`
- `POST /api/identity/members`
- `GET /api/elections`
- `POST /api/elections`
- `GET /api/votes/policy`
- `POST /api/votes`
- `POST /api/votes/cast`
- `GET /api/votes/tally`
- `GET /api/results/demo`
- `GET /api/results/{electionId}`
- `POST /api/results/{electionId}/publish`
- `GET /api/results/{electionId}/publication`

## Ballot Casting Rules

- The voter must be eligible for the election.
- The election must be open.
- The candidate must belong to the election.
- A voter can cast one accepted ballot per election.
- The response should return a receipt ID, not raw identity-linked ballot data.
