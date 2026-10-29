# BeforeYouGo

Know what to bring before you go. BeforeYouGo puts preparation guides for institution visits (government offices, colleges, hospitals, banks) in one place. Each guide has checklists, the steps on the day, a freshness badge showing how recently it was verified, and moderated visitor experiences.

This app is the payload for the DevOps CI/CD assignment: Git → Jenkins → Maven → Docker → ACR → AKS via Ansible. The Dockerfile, Jenkinsfile, Kubernetes manifests and Ansible playbooks are maintained separately against the **pipeline contract** below.

## Tech stack

- Java 21, Maven, Spring Boot 4.1.2
- Spring Web MVC + Thymeleaf (server-rendered; no Node build), plain CSS/JS in `src/main/resources/static`
- Spring Data JPA (Hibernate), H2 (default / tests) and PostgreSQL (`prod` profile)
- Spring Security (form login for `/admin/**`, CSRF on, BCrypt)
- Spring Boot Actuator (health + info, Kubernetes probes)
- Tests: JUnit, Mockito, Spring Boot Test (MockMvc, `@DataJpaTest`), JaCoCo coverage

## Run locally

```bash
mvn spring-boot:run
# or
mvn -B clean package
java -jar target/app.jar
```

Open http://localhost:8080. The admin area is at http://localhost:8080/admin. Local default credentials are **admin / admin123**, for the default profile only.

The default profile uses an in-memory H2 database and loads sample Chennai data on every start. All sample guides are UNVERIFIED, and the site shows "Sample data for demonstration — not official guidance."

## Tests and coverage

```bash
mvn -B clean test       # or: mvn -B clean verify
```

Reports:
- JUnit XML: `target/surefire-reports/*.xml`
- JaCoCo: `target/site/jacoco/jacoco.xml` (for Jenkins) and `target/site/jacoco/index.html`

The tests need no network services; they use H2 only.

## Pipeline contract

| Item | Value |
|---|---|
| Artifact | `target/app.jar` (executable, `<finalName>app</finalName>`) |
| Build | `mvn -B clean package` (runs the tests) |
| Port | `8080` |
| Liveness probe | `GET /actuator/health/liveness` (public) |
| Readiness probe | `GET /actuator/health/readiness` (public) |
| Health / info | `GET /actuator/health`, `GET /actuator/info` (public; no other actuator endpoints exposed) |
| Shutdown | graceful (`server.shutdown=graceful`, 20 s per phase) |
| Footer build tag | env `APP_VERSION` (default `local-dev`), shown as "BeforeYouGo · build <APP_VERSION>" on every page and in `/actuator/info` |

### Environment variables (`prod` profile)

Set `SPRING_PROFILES_ACTIVE=prod`. In prod every variable below is required, and the app **fails to start** if one is missing.

| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL` | e.g. `jdbc:postgresql://postgres:5432/beforeyougo` |
| `SPRING_DATASOURCE_USERNAME` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | DB password (from a Kubernetes Secret) |
| `ADMIN_USERNAME` | admin login (from a Kubernetes Secret) |
| `ADMIN_PASSWORD` | admin password (from a Kubernetes Secret); BCrypt-encoded in memory, never logged |
| `APP_VERSION` | optional; build tag shown in the footer |

The schema is managed by `spring.jpa.hibernate.ddl-auto=update`. Sample data is inserted only when the database is empty, so it survives redeploys. Set `APP_SEED_ENABLED=false` to turn seeding off.

## Features

- **Search and browse** (`/`): case-insensitive search over institution name, guide title and city, plus category chips.
- **Guide page** (`/guides/{id}`):
  - Institution details, purpose, and fees / appointment / timings. Missing values show "Not confirmed — check with the institution", never a guess.
  - Documents and prerequisites as a tickable checklist (client-side only).
  - Steps and common mistakes.
  - Freshness panel and a "Reported as possibly changed" banner.
- **Freshness**: `FreshnessCalculator`, driven by an injected `Clock`. FRESH up to 90 days, AGEING 91–180 days, POSSIBLY_OUTDATED after 180 days or when never verified.
- **Visitor experiences**:
  - Anonymous submissions (tip, discrepancy, outdated warning) are stored as PENDING.
  - Only approved ones are shown, newest first, in a separate section labelled as not official.
  - Approving a discrepancy or outdated warning flags the guide.
- **Admin** (`/admin`):
  - Dashboard with counts, the moderation queue and a "needs review" list.
  - Create and edit guides, with one list item per line.
  - Create institutions.
  - "Mark verified today", which sets OFFICIALLY_VERIFIED and today's date and clears the flag.
  - Logout.

## Project layout

```
src/main/java/com/beforeyougo
├── config/      AppConfig (Clock), SecurityConfig, DataSeeder, BuildInfo
├── domain/      Institution, ServiceGuide, Contribution + enums
├── repository/  Spring Data repositories (search query)
├── service/     FreshnessCalculator, GuideService, ContributionService
└── web/         PublicController, AdminController, ErrorHandling, form/ DTOs
src/main/resources
├── application.properties, application-prod.properties
├── templates/   Thymeleaf pages (+ admin/, error/, fragments/)
└── static/      css/app.css, js/checklist.js
```

## Limitations

- Sample data is illustrative, not official guidance.
- There is a single admin account, and visitors have no accounts. Moderation is the safeguard against bad submissions.
- `ddl-auto=update` keeps things simple; a production system would use versioned migrations.

## CI/CD pipeline

Push to GitHub → webhook (via ngrok) → Jenkins (WSL Ubuntu) → Maven compile, test, package → Docker image → Azure Container Registry → Ansible deploys to AKS → health and version check.

```
Jenkinsfile                 pipeline stages
Dockerfile, .dockerignore   runtime image (JRE 21, non-root), built from target/app.jar
k8s/                        namespace, PostgreSQL (PVC + Deployment + ClusterIP Service),
                            app Deployment (2 replicas, rolling update, probes), LoadBalancer Service
ansible/deploy.yml          deploys to AKS and verifies the new build is served
ansible/setup-build-host.yml  optional: the Jenkins host toolchain as code
ansible/inventory.ini       localhost (the Jenkins host)
docs/RUNBOOK.md             step-by-step setup, demo and cleanup
```

Secrets (database password, admin login) are stored as Jenkins credentials and written to a Kubernetes Secret by Ansible. They are never committed to the repository.
