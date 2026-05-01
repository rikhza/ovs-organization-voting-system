# OVS Technical Detail

## Architecture

OVS uses a deliberately small microservices architecture:

- `api-gateway`: serves the website and public API entry points.
- `identity-service`: owns users, roles, membership, and voter eligibility.
- `election-service`: owns elections, candidates, time windows, and election rules.
- `voting-service`: owns ballot casting, duplicate prevention, and vote audit events.
- `result-service`: owns tallies, result publication, and exports.
- `uvs-common`: shared API envelopes and cross-service primitives.

The MVP scaffold uses synchronous HTTP and simple demo endpoints. Production persistence should use PostgreSQL with separate schemas per service, managed by Flyway migrations.

## Why Simple Microservices

Voting has naturally separate trust boundaries, but an open-source project can become hard to maintain if every concern becomes infrastructure. OVS starts with clear service ownership, Dockerized boundaries, and shared contracts, while delaying service discovery, message brokers, and distributed tracing until they are needed.

## Recommended Production Data Ownership

- Identity database: organizations, users, roles, memberships, eligibility records.
- Election database: elections, candidates, ballot templates, schedules.
- Voting database: vote receipts, ballot selections, duplicate-prevention keys.
- Result database: tally snapshots, publication records, export history.

## Memory Safety And Resource Limits

- Java 21 provides memory-safe managed runtime behavior for application code.
- Containers set `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError`.
- Docker Compose applies `mem_limit` per service.
- Multipart upload limits are low by default.
- DTO validation should reject large or malformed request payloads before business logic.
- Future persistence work should use pagination for every list endpoint.

## API Design Rules

- Use DTOs and records for request and response payloads.
- Wrap responses with `ApiResponse`.
- Keep controllers thin.
- Put business rules in services.
- Use constructor injection and immutable dependencies.
- Use `@ConfigurationProperties` for configuration.
- Add global exception handling before exposing mutating APIs.

## Security Roadmap

1. Add Spring Security to the gateway and identity service.
2. Implement BCrypt password hashing or university SSO.
3. Add JWT or opaque session tokens.
4. Add role-based authorization per endpoint.
5. Add rate limits for login and vote-casting endpoints.
6. Add append-only audit log tables.
7. Add signed result snapshots.

## Local Ports

- API gateway: `8080`
- Identity service: `8081`
- Election service: `8082`
- Voting service: `8083`
- Result service: `8084`

## Version Choices

As of May 1, 2026, Spring's project listing shows Spring Boot `4.0.6+` as current. OVS uses Java 21 for broad LTS compatibility with organization and contributor machines.
