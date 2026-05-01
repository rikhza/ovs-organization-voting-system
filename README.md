# OVS - Organization Voting System

OVS is an open-source Java Spring Boot voting platform for student, campus, and community organizations. The default branding is `OVS`, and each deployment can override app name, organization name, and accent color from environment variables.

## Stack

- Java 21
- Spring Boot 4.0.6
- Maven multi-module project
- Docker Compose
- Static responsive web shell served by the API gateway

## Modules

- `apps/api-gateway`: modern web app and API gateway with demo auth, voter management, voting, and result publication
- `services/identity-service`: identity roles, demo login, and member management APIs
- `services/election-service`: election lifecycle and candidate list APIs
- `services/voting-service`: vote casting, duplicate-vote prevention, and tally APIs
- `services/result-service`: result snapshots and publication APIs
- `libs/common`: shared API response contracts

## Run Locally

```bash
mvn test
mvn spring-boot:run -pl apps/api-gateway
```

Open `http://localhost:8080`.

## Run With Docker

```bash
docker compose up --build
```

Customize the visible app name in `docker-compose.yml`:

```yaml
OVS_APP_NAME: "OVS"
OVS_ORGANIZATION_NAME: "Your Organization"
OVS_ACCENT_COLOR: "#0f766e"
```

## Project Docs

- [PRD](docs/PRD.md)
- [Technical Detail](docs/TECHNICAL_DETAIL.md)
- [API Contracts](docs/API_CONTRACTS.md)
