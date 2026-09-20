# NavVis Building Locator

A full-stack application for NavVis's take-home assignment: upload 3D building
data (polygonal outlines with height ranges and floors), then query any
(x, y, z) point to find which building and floor it falls within. Two locate
strategies are switchable at runtime via REST: ray casting in Java or spatial
filtering via PostGIS. Built with an emphasis on architecture and design
patterns over exhaustive feature coverage.

## Tech Stack

| Category | Technology                                         |
|-----------|----------------------------------------------------|
| **Language** | ☕ Java 25                                          |
| **Framework** | 🍃 Spring Boot 4                                   |
| **Build Tool** | ⚙️ Gradle (Groovy DSL)                             |
| **Database** | 🐘 PostgreSQL + PostGIS                            |
| **Persistence** | 🗃️ Hibernate + Hibernate Spatial, Spring Data JPA |
| **Migrations** | 🛫 Flyway                                          |
| **Frontend** | 🅰️ Angular 19                                     |
| **Containerization** | 🐳 Docker Compose                                  |
| **Code Formatting** | 🧹 Spotless                                        |
| **Testing** | 🧪 JUnit 5, Mockito, JaCoCo                        |
| **API Docs** | 📝 Spring REST Docs                                |

Single-module Gradle build. Backend and database run in Docker Compose;
the Angular frontend runs separately via `ng serve` with a dev proxy.

## Running it

### Environment

Create a `.env` file in the project root:

```
DB_NAME=navvis_locator
DB_USER=navvis
DB_PASSWORD=navvis
DB_HOST=localhost
DB_PORT=5432
CORS_ALLOWED_ORIGINS=http://localhost:4200
SERVER_PORT=8080
```

`.env` keeps credentials out of `docker-compose.yml` and version control
(gitignored). `docker compose` reads it automatically. Spring Boot consumes
the same variable names (`DB_NAME`, `DB_USER`, etc.) as plain OS environment
variables, so in production these would come from CI/CD, a secrets store,
or a cloud config service instead. No application code changes.

Note: `DB_HOST=localhost` is for the host machine. The `app` container
overrides this to `db` (the compose service name) in `docker-compose.yml`.

### Backend + Infrastructure

```bash
docker compose up --build
```

`--build` rebuilds the image when source has changed. Without it,
`docker compose up` reuses the last built image silently.

Starts PostgreSQL + PostGIS and Spring Boot at `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
ng serve
```

Runs at `http://localhost:4200`. A dev proxy (`proxy.conf.json`) forwards
`/api/*` to the backend.

### Database schema

Flyway migrations in `src/main/resources/db/migration/` are applied on
startup. The schema includes PostGIS geometry columns with a GIST spatial
index for the locate query.

## API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/buildings/upload` | Upload building data (multipart JSON file, 10MB limit) |
| `POST` | `/api/locate` | Find which building/floor contains a point |
| `GET` | `/api/strategy` | Get the current locate strategy |
| `PUT` | `/api/strategy` | Switch locate strategy at runtime |

## API documentation

Full REST API documentation is generated from the test suite itself via
Spring REST Docs. Every request/response example in the docs is verified
against real controller behavior as part of the build, so it cannot
drift out of sync with the code the way hand-written documentation can.

### Generate the docs
```bash
./gradlew test asciidoctor
```
This runs the tests (which produce documentation snippets from actual
HTTP exchanges), then compiles them into HTML and copies the output into
Spring's static resources. Once the app is running (`docker compose up`),
view it at: http://localhost:8080/docs/api-guide.html

```bash
# Upload building data
curl -X POST http://localhost:8080/api/buildings/upload \
  -F "file=@example_data.json"

# Locate a point
curl -X POST http://localhost:8080/api/locate \
  -H 'Content-Type: application/json' \
  -d '{"x": 15, "y": 15, "z": 1}'

# Check current strategy
curl http://localhost:8080/api/strategy

# Switch to PostGIS
curl -X PUT http://localhost:8080/api/strategy \
  -H 'Content-Type: application/json' \
  -d '{"strategy": "POSTGIS"}'
```

Upload accepts a multipart JSON file up to 10MB and returns
`{ "buildingsCreated": 5 }`. Files exceeding the limit receive a
`413 Payload Too Large` response. Locate returns
`{ "building": "Office", "floor": "Ground" }` with null values when
the point is not inside any building or floor. All error responses
follow RFC 7807 Problem Detail format.

## Architecture decisions

### Hexagonal architecture (ports & adapters)

The domain layer (`Building`, `Floor`, `Polygon2D`, `HeightRange`)
has zero framework dependencies. All Spring, JPA, and Jackson concerns
live in the adapter layer. The domain communicates with the outside
world through ports: `LocatePointUseCase` and `UploadBuildingsUseCase`
as inbound ports (driven by REST controllers), `BuildingRepository`
and `BuildingDataParser` as outbound ports (implemented by JPA and
Jackson adapters).

This earns its keep at the strategy boundary. Adding a new locate
strategy (e.g. an in-memory R-tree) means implementing `BuildingLocator`,
dropping it in, and registering the enum value. The domain model, ports,
sealed results, visitor, and every existing strategy remain untouched.
That is the concrete payoff of the hexagonal split, not an abstract
"clean architecture" benefit.

### Three locate outcomes as a sealed interface

The locate endpoint does not return a success/failure boolean or
overloaded nulls. Three outcomes are modelled as a sealed interface
(`LocationResult`):

- `Located(building, floor)` -- point is on a specific floor
- `BuildingOnly(building)` -- inside the building envelope but between
  floors, or outside every floor's polygon
- `NotFound()` -- not inside any building

The sealed interface forces every consumer to handle all three cases
at compile time. Mapping to DTOs uses the Visitor pattern: each variant
dispatches to `LocationResponseMapper`, so the controller contains no
switch statement and adding a new outcome is a compile error in every
visitor implementation until handled.

### Design patterns

**Strategy pattern** for locate execution. `BuildingLocator` is the
interface; `JavaRayCastingLocator` and `PostgisLocator` are the
implementations. `BuildingLocatorResolver` holds a map of all
registered strategies and resolves the active one via
`LocateStrategyToggle`. The service layer calls
`locatorResolver.current().locate(x, y, z)` with no awareness of
which implementation runs. Adding a third strategy means one new class
and one enum value.

**Visitor pattern** for result mapping. `LocationResult` defines
`accept(Visitor<T>)`, and each sealed variant dispatches to the
corresponding `visit()` method. `LocationResponseMapper` implements
the visitor to produce `LocateResponse` DTOs. This eliminates switch
statements in the web layer and guarantees compile-time exhaustiveness
when a new variant is added.

### Feature toggle: Java ray casting vs PostGIS

Both locate strategies are available at runtime, switchable via
`PUT /api/strategy` without restart. This is deliberate: each approach
has trade-offs worth demonstrating, and the toggle lets reviewers
compare them against the same dataset.

**Java ray casting** loads candidate buildings by height range from the
database, then checks each polygon in the application layer using a
ray casting algorithm (`Polygon2D`). Testable without a database and
portable across any storage backend, but candidate set grows linearly
with building count.

**PostGIS (`ST_Covers`)** pushes the polygon check to the database
using a GIST-indexed spatial query. O(log N) regardless of dataset size,
no entity materialization for non-matches. Trades portability for
performance. `ST_Covers` is used instead of `ST_Contains` because
`ST_Contains` excludes points on polygon edges and vertices.

### Why PostgreSQL + PostGIS, not just PostgreSQL

PostGIS is the load-bearing reason. The Java strategy is correct and
clean but does not scale. At millions of buildings, the height-only
database filter returns too many candidates. The PostGIS strategy uses
`ST_Covers` with a GIST spatial index, making it O(log N) regardless
of dataset size. Choosing PostGIS from the start means the schema
already includes the spatial index and geometry columns, so the
strategy is available without a database migration under production load.

The honest tradeoff: PostGIS adds a non-trivial extension dependency
(the Docker image is `postgis/postgis`, not plain `postgres`), and
Hibernate Spatial is required to map JTS geometry types correctly.
Without Hibernate Spatial, Hibernate falls back to Java serialization
for geometry columns, and PostGIS throws opaque "Invalid endian flag"
errors on read, a bug that is hard to diagnose without knowing the
cause.

### Why Flyway, not Hibernate auto-DDL

`spring.jpa.hibernate.ddl-auto` is set to `validate`, not `update` or
`create`. Hibernate validates that the entity mappings match the actual
schema but never modifies it. All schema changes go through Flyway
migrations in `src/main/resources/db/migration/`, versioned and applied
in order on startup.

This is a deliberate choice: Hibernate's `update` mode silently alters
tables in ways that are not reversible, not auditable, and not
reproducible across environments. A column type change or a dropped
constraint in production with no migration file and no review process
is how data gets lost. Flyway migrations are SQL files checked into
version control, reviewed in pull requests, and applied identically in
every environment from local dev to production. The tradeoff is that
every schema change requires writing a migration file by hand, but
that is the point: schema changes should be intentional, not automatic.

### File size limit

Uploads are capped at 10MB, enforced at two layers:

- **Frontend:** validates file size before sending. Files over the limit
  are rejected with a message immediately, no network request made.
- **Backend:** Spring's multipart resolver rejects oversized requests
  before they reach the controller. The `GlobalExceptionHandler` catches
  `MaxUploadSizeExceededException` and returns a `413 Payload Too Large`
  response in RFC 7807 Problem Detail format.

At ~1.3KB per building in the current JSON format, the 10MB limit
accommodates roughly 7,500 buildings per upload.

### Batch insert

Building uploads are persisted using `saveAll()` with Hibernate JDBC
batching (`batch_size: 100`). This groups inserts into batches of 100
rather than issuing one round trip per building. At the 10MB file limit
(~7,500 buildings), this reduces database round trips from 7,500 to 75.

### Spotless

Spotless enforces consistent formatting across the codebase via
`./gradlew spotlessCheck` (verify) and `./gradlew spotlessApply` (fix).
The current rules remove unused imports, trim trailing whitespace, and
ensure files end with a newline. Runs as a Gradle plugin with no IDE
configuration required.

## How locate works

The locate endpoint delegates to whichever `BuildingLocator`
implementation is active via the strategy toggle.

### Java ray casting strategy

1. **Database stage:** a B-tree indexed query filters buildings whose
   height range contains the z coordinate. This eliminates most buildings
   cheaply.

2. **Application stage:** each candidate building is checked with a
   ray casting algorithm. A horizontal ray is shot from the query point
   to the right, and edge crossings with the building's 2D polygon
   outline are counted. An odd count means the point is inside. This is
   O(n) in the number of polygon edges, but building outlines have
   single-digit edge counts, so "O(n)" means roughly 6 iterations.

3. **Floor lookup:** once inside a building, each floor is checked with
   the same height + ray casting test. Floors can have different outlines
   than the building (e.g. upper floors with setback polygons), producing
   the `BuildingOnly` result when a point is inside the building but
   outside every floor's outline.

### PostGIS strategy

1. **Database stage:** a single spatial query uses `ST_Covers` with the
   GIST index to find buildings whose polygon contains the (x, y) point
   and whose height range contains z. Returns only matching rows with no
   candidate set in Java.

2. **Floor lookup:** identical to the Java strategy. Floor polygons are
   checked in the domain layer since floors are nested within buildings
   and benefit less from spatial indexing.

## Testing strategy

Unit tests (JUnit 5 + Mockito) cover the domain model, application
services, and adapters, with collaborators mocked. Controller tests use
`@WebMvcTest` with the use-case layer mocked, verifying HTTP status
codes, response structure, and validation behavior. JaCoCo generates
coverage reports via `./gradlew test jacocoTestReport`.

| Layer | What's tested | Style |
|-------|---------------|-------|
| Domain model | Ray casting (`Polygon2D`), height range boundaries, building containment + floor lookup | Plain JUnit, no Spring |
| Application services | `LocationService` three outcomes, `BuildingUploadService` submission, `UploadProcessingService` success + failure | Mockito mocks for ports |
| Application strategy | Both strategies return identical results for the same input | Mockito mocks for repository |
| Controllers | `LocationController` all three `LocationResult` cases + validation, `BuildingUploadController` upload + missing file, `LocateStrategyController` get + switch + invalid | `@WebMvcTest` + MockMvc |
| Adapters | `JacksonBuildingDataParser` parsing + containment | Plain JUnit |
| End-to-end | Upload acceptance, file size rejection, strategy performance comparison, result equivalence | Shell scripts over HTTP |

Test coverage (JaCoCo): **65% overall, 100% branch coverage**.
Domain model, application services, and web controllers are at 100%.
The persistence adapter (0%) requires integration tests against a real
PostGIS instance via Testcontainers (`@DataJpaTest`), which was scoped
out in favor of covering the domain and application layers thoroughly
with fast, isolated unit tests. The persistence layer is instead
verified by the shell scripts in `scripts/`, which exercise the full
upload and locate flow over HTTP against the real database.

### Smoke and load tests

Shell-based test scripts live in `scripts/`, runnable against a live
instance with no test framework dependencies.

**`scripts/load-test-strategy.sh`** uploads 50 buildings
(`scripts/inputs/test-buildings.json`), runs N locate requests against
each strategy, reports per-strategy timing (avg/min/max), and verifies
both strategies return identical results. Configurable via `HOST` and
`ITERATIONS` environment variables.

**`scripts/smoke-test-upload.sh`** validates upload behavior: a valid
file is accepted, a missing file part is rejected with 400, and an
oversized file (11MB, generated at runtime) is rejected with 413.

These scripts complement the JUnit suite by testing contracts that only
surface over real HTTP: multipart size enforcement in the servlet
container, spatial query correctness against a real PostGIS instance,
and performance characteristics under repeated load.

## Performance and scaling

### Performance comparison

Load tested with 50 buildings, 50 iterations per strategy:

| Strategy | Avg | Min | Max | Total |
|----------|-----|-----|-----|-------|
| Java ray casting | 12ms | 9ms | 34ms | 647ms |
| PostGIS | 4ms | 4ms | 8ms | 229ms |

PostGIS is roughly 3x faster with 50 buildings. The gap widens with
dataset size: the Java strategy's candidate set grows linearly while
PostGIS stays O(log N) via the GIST index. Both strategies return
identical results for every query point tested.

### How far each strategy stretches without caching

**Java ray casting** is bottlenecked by Tomcat threads. Each request
holds a thread for the full duration: database query plus ray casting.
With 200 default Tomcat threads:

- 50 buildings (12ms/req): ~16,000 req/s
- 500 buildings (~120ms/req): ~1,600 req/s
- 5,000 buildings (~1.2s/req): ~160 req/s

Throughput degrades linearly with building count because every
candidate returned by the height filter is ray-cast on the servlet
thread. At 5,000 buildings, a single instance cannot sustain even
14 million requests per day.

**PostGIS** is bottlenecked by the connection pool, not threads.
Each request holds a thread for ~5ms regardless of building count,
so Tomcat threads are never the limit. The ceiling is HikariCP:

- 10 connections (default), 4ms/query: ~2,500 req/s (216M req/day)
- 20 connections: ~5,000 req/s (432M req/day)
- Beyond ~50 connections, PostgreSQL CPU becomes the limit

A single PostgreSQL instance handles roughly 3,000-5,000 spatial
queries per second. Adding a read replica doubles that. At that point,
caching or application-level sharding is the next step, but the
ceiling is well above most real-world locate workloads.

### Multi-instance scaling

PostgreSQL uses a process-per-connection model, more expensive per
connection than thread-per-connection databases. At multi-instance
scale with many application servers, the standard fix is PgBouncer
as a connection pooler between the application and the database.
Each application instance connects to PgBouncer, which multiplexes
onto a smaller pool of actual database connections.

## What production readiness needs

The application is functionally complete for the assignment scope. Moving
it to production would require work in these areas:

**Strategy readiness.** The PostGIS strategy is close to production as-is:
`ST_Covers` with a GIST index is the standard approach for spatial
lookups, scaling to millions of buildings with no application code
changes. The Java ray casting strategy is correct and fully tested but
would need an in-memory spatial index (R-tree or grid partitioning) to
avoid materializing large candidate sets at scale. In production, the
toggle would be set to PostGIS permanently, with the Java strategy
retained for environments without PostGIS or for debugging.

**Frontend.** The Angular app is intentionally minimal -- the assignment's
weight is on backend architecture, design patterns, and spatial logic.
It proves the API works end-to-end (file upload, point query, strategy
toggle) but lacks error recovery, loading states, and responsive design.
For independent deployment: `ng build` produces static assets that can be
served from nginx or a CDN with the API base URL injected as an
environment variable at container startup, removing the dev proxy
dependency and allowing frontend and backend to scale and deploy
separately.

**Integration tests.** Repository tests (`@DataJpaTest` with
Testcontainers against a real PostGIS instance) and full end-to-end
tests (upload a file, locate a point, verify the result) are absent.
The domain and application layers have unit coverage; the controller
slices verify Spring wiring and HTTP contracts. The integration layer
between them is tested only by the shell scripts and by running the
application.

**Observability.** Structured JSON logging with request correlation IDs
(e.g. Micrometer Tracing) so a single request can be traced through the
controller, service, and database layers. Health check endpoints
(`/actuator/health`) with custom indicators for database connectivity
and PostGIS extension availability. Metrics export (Prometheus) for
request latency, error rates, and connection pool utilization.

**Security.** Authentication (JWT or API key) on all endpoints.
Authorization to scope uploads and strategy changes to admin roles.
Rate limiting on the locate endpoint to prevent abuse. CORS locked to
known origins rather than a configurable wildcard.

**Operational.** CI/CD pipeline running tests, Spotless formatting
checks, and JaCoCo coverage gates before merge. Container image pushed
to a registry rather than built on the host. Database backups and
migration dry-runs in staging before production.

**Data integrity.** Input validation during upload: required fields,
valid coordinate ranges, non-degenerate polygons (at least 3 vertices,
no self-intersections), with per-building error reporting rather than
failing the entire upload. Idempotency on upload to prevent duplicate
buildings from retried requests.