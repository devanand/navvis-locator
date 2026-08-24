# NavVis Building Locator

A full-stack application for NavVis's take-home assignment: upload 3D
building data (polygonal outlines with height ranges and floors), then
query any (x, y, z) point to find which building and floor it falls
within. Upload is asynchronous via Kafka; the locate query uses a
two-stage spatial filter, a database height check followed by ray
casting in the domain layer. Built with an emphasis on architecture and
production-readiness reasoning over exhaustive feature coverage.

## Tech Stack

| Category | Technology |
|-----------|------------|
| **Language** | ☕ Java 21 |
| **Framework** | 🍃 Spring Boot 4 |
| **Build Tool** | ⚙️ Gradle (Groovy DSL) |
| **Database** | 🐘 PostgreSQL + PostGIS |
| **Persistence** | 🗃️ Hibernate + Hibernate Spatial, Spring Data JPA |
| **Database Migrations** | 🛫 Flyway |
| **Messaging** | 📨 Apache Kafka (KRaft mode) |
| **Frontend** | 🅰️ Angular 19 |
| **Containerization** | 🐳 Docker Compose |
| **Testing** | 🧪 JUnit 5, Mockito, JaCoCo |


Single-module Gradle build. The backend, database, and Kafka broker all
run inside Docker Compose; the Angular frontend runs separately via
`ng serve` with a dev proxy to the backend.

## Running it

### Environment

Create a `.env` file in the project root:

```
DB_NAME=navvis_locator
DB_USER=navvis
DB_PASSWORD=navvis
DB_HOST=localhost
DB_PORT=5432
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
CORS_ALLOWED_ORIGINS=http://localhost:4200
SERVER_PORT=8080
KAFKA_LISTENER_CONCURRENCY=1
```

A `.env` file is needed to run this locally at all. `docker compose`
reads it automatically and injects the values as container environment
variables, and without it the containers have no credentials to start
with.

Using it is a deliberate choice, not just convenience: it keeps
credentials out of `docker-compose.yml` and out of version control
(`.env` is gitignored) while still being the *same* mechanism a real
deployment would use. Spring Boot reads `DB_NAME`/`DB_USER`/etc. as
plain OS environment variables regardless of who sets them. Locally,
that's `.env`; in production, the identical variable names would be set
by CI/CD, the container orchestrator's secrets store, or a cloud
provider's config service instead. The application code has no awareness
of which one supplied them.

Note `DB_HOST=localhost` here, not `db`: the `app` container's own
`docker-compose.yml` environment block overrides it to use the compose
service name `db`, since inside the compose network hostnames are
service names, not `localhost`. 

Similarly, `KAFKA_BOOTSTRAP_SERVERS=kafka:9092` matches the compose service name
and is also hardcoded in the compose environment block. These `.env`
values only matter if you run the app outside compose (e.g. from an
IDE), where you would change `KAFKA_BOOTSTRAP_SERVERS` to
`localhost:9092` to reach the compose-managed Kafka broker on the
host's mapped port.

### Backend + Infrastructure

```bash
docker compose up --build
```

`--build` matters on every run where the source changed.
`docker compose up` alone reuses the existing image if one exists, so
code changes without `--build` silently run against stale,
already-built code.

This starts PostgreSQL + PostGIS, Kafka (single-node KRaft), and the
Spring Boot application. The API is available at `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
ng serve
```

The Angular app runs at `http://localhost:4200` with a dev proxy
forwarding `/api/*` to the backend. The proxy is configured in
`proxy.conf.json` and referenced from `angular.json`. Without it,
requests go to the Angular dev server itself instead of the backend.

### Database schema

Flyway migrations live at `src/main/resources/db/migration/`, applied
automatically on startup. The schema uses PostGIS geometry columns for
building and floor outlines, with a GIST spatial index that becomes
load-bearing at production scale (see Scaling below).

## API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/buildings/upload` | Upload building data (multipart JSON file) |
| `GET` | `/api/buildings/upload/{jobId}/status` | Poll upload job status |
| `POST` | `/api/locate` | Find which building/floor contains a point |

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

The curl examples in the API section above are a quick-reference
shortcut, not a substitute for the generated docs. Field descriptions,
validation rules, and all three locate outcomes are documented there
with verified examples.

```bash
# Upload building data
curl -X POST http://localhost:8080/api/buildings/upload \
  -F "file=@example_data.json"

# Check upload status (jobId from the response above)
curl http://localhost:8080/api/buildings/upload/{jobId}/status

# Locate a point
curl -X POST http://localhost:8080/api/locate \
  -H 'Content-Type: application/json' \
  -d '{"x": 15, "y": 15, "z": 1}'
```

Upload returns `202` with `{ "jobId": "uuid" }`. The file is published
to Kafka and processed asynchronously. Status returns
`PENDING | PROCESSING | DONE | FAILED`. Locate returns
`{ "building": "Office building", "floor": "Floor 0" }` with null
values when the point is not inside any building or floor.

The job ID is a UUID exposed directly in the URL. This is deliberate:
UUIDs are unguessable, which prevents Insecure Direct Object Reference
(IDOR). A sequential integer would let any client enumerate every other
user's upload jobs just by incrementing the ID. The tradeoff is URL
aesthetics, which is worth it for a backend API that humans rarely see
in a browser bar.

## Architecture decisions

### Hexagonal architecture (ports & adapters)

The domain layer (`Building`, `Floor`, `Polygon2D`, `HeightRange`,
`UploadJob`) has zero framework dependencies. All Spring, JPA, Kafka,
and Jackson concerns live in the adapter layer. The domain communicates
with the outside world through ports: `UploadBuildingsUseCase` and
`LocatePointUseCase` as inbound ports (driven by REST controllers and
Kafka consumers), `BuildingRepository` and `BuildingUploadPublisher` as
outbound ports (implemented by JPA and Kafka adapters).

```
com.navvis.locator
├── domain
│   ├── model          Building, Floor, Polygon2D, HeightRange, UploadJob
│   └── port
│       ├── in         UploadBuildingsUseCase, LocatePointUseCase
│       └── out        BuildingRepository, BuildingUploadPublisher
├── application
│   ├── service        BuildingUploadService, LocationService
│   └── processing     UploadProcessingService
├── adapter
│   ├── in
│   │   ├── web        REST controllers + DTOs
│   │   └── messaging  KafkaBuildingUploadConsumer
│   └── out
│       ├── persistence JPA entities, repositories, mapper
│       ├── messaging   KafkaBuildingUploadPublisher
│       └── parser      JacksonBuildingDataParser
└── config             MessagingConfig, PersistenceConfig, CorsConfig
```

This earns its keep at the scaling boundary. When the locate query needs
to push polygon checks to PostGIS (see Scaling), only
`JpaBuildingRepository.findContaining()` changes. The domain model,
ports, sealed results, and the entire Kafka pipeline remain untouched.
That is the concrete payoff of the hexagonal split, not an abstract
"clean architecture" benefit.

### Three locate outcomes as a sealed interface

The locate endpoint does not return a success/failure boolean or
overloaded nulls. Three outcomes are modelled as a **sealed interface**
(`LocationResult`):

- `Located(building, floor)` -- point is on a specific floor
- `BuildingOnly(building)` -- inside the building envelope but between
  floors, or outside every floor's polygon (e.g. Floor 4 in the example
  data has a smaller "setback" outline)
- `NotFound()` -- not inside any building

This is a deliberate choice: the `BuildingOnly` case is a real outcome
the example data produces (query a point between floors, or inside the
building outline but outside Floor 4's setback), and collapsing it into
either "found" or "not found" loses information the caller needs. The
sealed interface forces every consumer (the REST controller, any future
gRPC adapter) to handle all three cases explicitly at compile time.

## Technology choices

### Why PostgreSQL + PostGIS, not just PostgreSQL

PostGIS is the load-bearing reason. The locate query's current
implementation keeps polygon logic in the Java domain layer (ray
casting), which is correct and clean but does not scale. At 12M
buildings, the height-only database filter returns too many candidates
(see Scaling). The production fix is `ST_Contains` with a GIST spatial
index, and that requires PostGIS. Choosing it now means the schema
migration already includes the spatial index and geometry columns, so
the fix is a single repository method change, not a database migration
under production load.

The honest tradeoff: PostGIS adds a non-trivial extension dependency
(the Docker image is `postgis/postgis`, not plain `postgres`), and
Hibernate Spatial is required to map JTS geometry types correctly.
Without Hibernate Spatial, Hibernate falls back to Java serialization
for geometry columns, and PostGIS throws opaque "Invalid endian flag"
errors on read, a bug that is hard to diagnose without knowing the
cause.

PostgreSQL also uses a process-per-connection model, more expensive per
connection than MySQL/MariaDB's thread-per-connection model. Not an
issue at single-instance scale since HikariCP opens a small number of
long-lived connections and reuses them. At multi-instance scale, the
standard fix is PgBouncer (see Scaling). MySQL and MariaDB both have
built-in spatial support (`ST_Contains`, R-tree indexes) that would
work for this project's point-in-polygon queries, but PostGIS is a
significantly more capable spatial engine: 3D geometry operations,
coordinate system transformations, geography types that account for
earth's curvature. Switching databases to solve a connection pooling
problem that PgBouncer solves in an afternoon is not a tradeoff worth
making.

### Why Kafka for uploads, not synchronous processing

The upload endpoint returns `202 Accepted` with a job ID immediately,
then publishes the file content to a Kafka topic for asynchronous
processing. This is not premature optimization; it is a design signal
about how uploads behave in production.

Upload files can be large and parsing + persisting thousands of
buildings is not something that belongs in a synchronous HTTP
request-response cycle. A 30-second parse ties up a servlet thread and
leaves the client hanging. The consumer (`KafkaBuildingUploadConsumer`)
and its processing logic (`UploadProcessingService`) live in a separate
package, designed for extraction as an independent microservice when
upload volume justifies it. The Kafka topic is the only contract between
producer and consumer. Job status tracking (`UploadJob` with
`PENDING -> PROCESSING -> DONE | FAILED` state machine) gives the
client a polling mechanism that works identically whether the consumer
runs in-process or as a separate service.

The honest tradeoff: for this assignment's data volume (one file, one
building), synchronous processing would work fine and would be simpler.
Kafka adds operational complexity (a broker to run, consumer group
management, offset tracking). The choice is about demonstrating
production thinking, not about the example data's actual scale.

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

## How locate works

The point location algorithm uses a two-stage spatial filter:

1. **Database stage:** a B-tree indexed query filters buildings whose
   height range contains the z coordinate. This eliminates most buildings
   cheaply. Commercial buildings cluster in similar height ranges, but
   the z coordinate still rules out everything that is physically above
   or below the query point.

2. **Application stage:** each candidate building is checked with a
   **ray casting algorithm**. A horizontal ray is shot from the query
   point to the right, and edge crossings with the building's 2D polygon
   outline are counted. An odd count means the point is inside. This is
   O(n) in the number of polygon edges, but building outlines have
   single-digit edge counts, so "O(n)" means ~6 iterations.

3. **Floor lookup:** once inside a building, each floor is checked with
   the same height + ray casting test. Floors can have different outlines
   than the building. Floor 4 in the example data has a smaller
   "setback" polygon, so a point inside the building outline at that
   height can still be outside Floor 4's outline, producing the
   `BuildingOnly` result.

The ray casting implementation lives in the domain layer (`Polygon2D`),
not in the database. This is a conscious trade-off: the algorithm stays
testable, portable, and framework-free, at the cost of not scaling
beyond ~100K buildings (see Scaling for the fix and why the hexagonal
architecture makes it a single-method change).

## Kafka configuration

All Kafka configuration lives in `application.yaml`: bootstrap servers,
serializers, consumer group ID, topic names. The Java adapters reference
these via Spring property placeholders
(`${app.kafka.topics.building-uploads}`,
`${spring.kafka.consumer.group-id}`), never hardcoded strings.

Topic management (partition count, replication factor, retention) is
deliberately absent from the application. In production, topics are
created and managed via Terraform or the managed service console (AWS
MSK, Confluent Cloud), not by application code. For local development,
Kafka auto-creates topics with default settings, which is sufficient.

One non-obvious detail: `MessagingConfig` defines manual
`ProducerFactory`, `ConsumerFactory`, `KafkaTemplate`, and
`KafkaListenerContainerFactory` beans rather than relying on Spring
Boot's Kafka auto-configuration. This is forced, not chosen. Spring
Boot auto-configuration creates a `KafkaTemplate<Object, Object>`, but
this application needs `KafkaTemplate<String, byte[]>`. Spring's generic
type matching rejects the mismatch at injection time. More importantly,
once a custom `ProducerFactory` is present, Spring auto-configuration
backs off *entirely*, including consumer-side beans, so the config must
provide the full set or the `@KafkaListener` container factory is
missing at startup. The Javadoc on `MessagingConfig` explains this so
the next developer does not delete the "redundant" beans and break the
consumer.

**Consumer scaling:** the number of Kafka partitions is the ceiling on
consumer parallelism. `spring.kafka.listener.concurrency` (defaulting
to 1, overridable via `KAFKA_LISTENER_CONCURRENCY`) controls how many
consumer threads each instance runs, but more threads than partitions
means idle threads, not more throughput.

## Testing strategy

Unit tests (JUnit 5 + Mockito) cover the domain model, application
services, and adapters, with collaborators mocked. Controller tests use
`@WebMvcTest` with the use-case layer mocked, verifying HTTP status
codes, response structure, and validation behavior. JaCoCo generates
coverage reports via `./gradlew test jacocoTestReport`.

| Layer | What's tested | Style |
|-------|---------------|-------|
| Domain model | Ray casting (`Polygon2D`), height range boundaries, building containment + floor lookup, `UploadJob` state machine immutability | Plain JUnit, no Spring |
| Application services | `LocationService` three outcomes, `BuildingUploadService` submit + status + not found, `UploadProcessingService` success + failure + missing job | Mockito mocks for ports |
| Controllers | `LocationController` all three `LocationResult` cases + validation, `BuildingUploadController` upload + status + missing file + unknown job | `@WebMvcTest` + MockMvc |
| Adapters | `KafkaBuildingUploadConsumer` delegation + invalid UUID, `KafkaBuildingUploadPublisher` topic + key, `JacksonBuildingDataParser` parsing + setback + containment | Mockito or plain JUnit |

Deliberately not included: integration tests that hit a real database
or Kafka broker. The natural next step is Testcontainers spinning up
PostgreSQL + PostGIS for repository tests (`@DataJpaTest`) and a full
`@SpringBootTest` for the upload-to-locate flow end to end, plus
`@EmbeddedKafka` to verify that a message published by the producer is
actually received by the consumer. These were scoped out in favor of
covering more of the domain and adapter logic with fast, isolated tests.

## Known limitations / what I'd do with more time

**No tests beyond unit and controller slices.** Repository tests
(`@DataJpaTest` with Testcontainers against a real PostGIS instance),
Kafka integration tests (`@EmbeddedKafka` for the full
produce-consume-persist cycle), and end-to-end tests (upload a file,
poll until DONE, locate a point) are all absent. The domain layer's ray
casting and the upload job state machine have unit coverage; the
controller slices verify Spring wiring and HTTP contracts. The
integration layer between them is tested only by running the application.

**No consumer error handling.** If the Kafka consumer fails
mid-processing (malformed JSON, database down, unexpected schema), the
message is not retried or dead-lettered. It is simply lost. A production
consumer would need a dead-letter topic, a retry policy with backoff,
and an error table that records what failed and why, so operators can
inspect and replay.

**No file validation during processing.** The consumer assumes the
uploaded JSON is structurally correct and throws a
`NullPointerException` on malformed input. A production system should
validate the file during processing: check for required fields, valid
coordinate ranges, non-degenerate polygons (at least 3 vertices, no
self-intersections), and return structured errors per building rather
than failing the entire job.

**Upload tracking relies on a UUID the user cannot recover.** The upload
endpoint returns a job ID that the client must store to poll status. If
the client loses it, there is no way to find the job again. A production
system should accept a user-provided name or address for each upload,
store it alongside the job, and let the user query by name. The UUID
stays the internal identifier; the name is what humans use to find their
upload.

**Polling instead of notification.** The client polls
`GET /status/{jobId}` to check whether processing is done. This works
but wastes requests when the job takes time and adds latency when the job
finishes between polls. A production system would notify the client on
completion: WebSocket push, server-sent events, or a webhook URL
provided at upload time. Polling would remain as a fallback for clients
that cannot accept push connections.

**No authentication or authorization.** Any client can upload, query
status, or locate. The UUID job ID provides obscurity (unguessable), not
security. A production system needs authentication (JWT or API key),
tenant-scoped queries, and rate limiting.

**No input validation on locate.** The endpoint accepts any numeric
x/y/z without bounds checking. In production, coordinates outside the
expected geographic range should be rejected early rather than queried
against the database.

**No logging configuration.** The application uses Spring Boot's default
logging (Logback with console output). There are no structured log
statements in the service or adapter layers, no correlation IDs for
tracing a request through Kafka, and no log level configuration per
package. A production deployment needs structured JSON logging, request
correlation IDs, and log aggregation (ELK, Datadog, or CloudWatch).

**Frontend is minimal.** The Angular app is a functional proof that the
API works end-to-end (file upload with status polling, point query with
result display) but has no error recovery, loading states, or responsive
design beyond basic layout. A production frontend would show upload
progress, display structured errors from failed jobs, remember recent
queries, and visualize building outlines on a map.

## Scaling

### Current performance

With the example data (1 building, 6 floors), a single instance handles
roughly 3,000 to 5,000 locate requests per second. The database round
trip (~2ms per query) dominates; the ray casting itself is pure
arithmetic on a handful of edges and contributes negligibly.

The bottleneck is the HikariCP connection pool (10 connections by
default). Each locate query holds a connection for ~2ms, so maximum
throughput is `10 connections x (1000ms / 2ms) = 5,000 req/s`.
Increasing the pool size helps linearly until the database's own CPU
becomes the limit.

### Where it breaks

The current design keeps polygon logic in the Java domain layer (ray
casting), which is correct and testable but does not scale. The
height-only database filter returns increasingly large candidate sets as
buildings grow. Commercial buildings cluster in similar height ranges (3
to 30m), so at millions of buildings, a single query could match
thousands or more, all materialized as JPA entities, mapped to domain
objects, and ray-cast checked in Java. Memory allocation and processing
time grow linearly with the candidate set.

This is a conscious tradeoff: clean hexagonal boundaries now, with a
known fix scoped for when scale demands it.

### The fix: push polygon check to PostGIS

Replace the height-only query plus Java ray casting with a single
spatial query:

```sql
SELECT * FROM buildings
WHERE ST_Contains(outline, ST_Point(:x, :y))
  AND height_min <= :z AND height_max >= :z
```

The GIST index (already present in the schema migration) makes
`ST_Contains` O(log N). The query returns 0 to 1 rows in a few
milliseconds regardless of dataset size. Only
`JpaBuildingRepository.findContaining()` changes. The domain model,
ports, sealed interface, and the entire Kafka pipeline remain untouched.

This is the hexagonal payoff: a single repository method absorbs what
would otherwise be a system-wide refactor.
