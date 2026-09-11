# Build Prompt: Fasol Doctor Backend (Spring Boot only)

## Goal
Build **only the backend** — a Spring Boot REST API that authenticates farmers/experts/admins, receives diagnosis requests, calls an external AI prediction service, matches results to a verified treatment database, stores diagnosis history, and supports farmer↔expert messaging. **Do not build a frontend** (no Flutter, no web UI) — this is a backend-only build. The API will be consumed by a mobile app later, but that's out of scope for this task.

## What already exists (do not rebuild)
An external AI model service is already running and must be treated as a black box:

| | |
|---|---|
| Base URL | `http://10.100.59.183:8000` (private/local-network address — your Spring Boot server must be able to reach this address; confirm with `GET /health` before building anything against it) |
| Auth | `X-API-Key: <key>` header on prediction endpoints; `/health` needs no auth |
| Response language | Bangla |

**Endpoints on the AI service:**
- `GET /health` → `{"status": "ok", "service": "Fasol Doctor AI"}`
- `POST /predict/text` — `Content-Type: application/json`, body `{"text": "..."}`
- `POST /predict/image` — `multipart/form-data`, field name **must be** `image`

**Success response (identical shape for both prediction endpoints):**
```json
{
  "disease": "রাইস টুংরো",
  "confidence": 86.43,
  "needsExpertReview": false,
  "message": "রোগটি সফলভাবে শনাক্ত করা হয়েছে।"
}
```

**Error responses from the AI service:** `401` missing/invalid API key · `400` empty text · `400` invalid image type · `400` unreadable image · `400` poor/blurry image quality (the AI service already does blur detection itself — you don't need to build any image-quality checking, just forward its error message).

**Confidence threshold:** 70%, decided by the AI service — `needsExpertReview` comes back as `true` below that. Your backend just trusts the flag, no recomputation needed.

**Supported disease classes (exact Bangla strings returned in `disease` — use these for exact-match lookups):**
| Model class | Bangla API response |
|---|---|
| Blast | ব্লাস্ট |
| Brown spot | ব্রাউন স্পট |
| Healthy | সুস্থ |
| Leaf smut | লিফ স্মাট |
| Rice Tungro | রাইস টুংরো |
| Sheath blight | শীথ ব্লাইট |

## Tech stack
- **Java 17+, Spring Boot 3.x, Maven**
- `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`
- `com.h2database:h2` — file-mode H2 database (zero-setup for now; same JPA entities work against Postgres later by only changing the datasource config)
- `io.jsonwebtoken:jjwt-*` — JWT issuance/validation
- Spring's `RestClient` (or `RestTemplate`) for calling the external AI service

## Architecture
```
(Future mobile client — not built in this task)
     │  JWT bearer token
     ▼
Spring Boot backend  ← THIS IS WHAT YOU'RE BUILDING
 ├── /auth/*            — register, login, JWT issuance
 ├── /predict/*         — calls the external AI service, matches disease, looks up treatment, logs diagnosis
 ├── /diagnoses/*        — history, detail, outcome feedback
 ├── /crops/*, /diseases/*, /treatments/* — read for all users, write for admin only
 └── /conversations/*   — farmer ↔ expert messaging
     │
     ├── H2 database (users, crops, diseases, treatments, diagnoses, conversations, messages)
     └── External AI service (10.100.59.183:8000) — called only from the prediction layer
```

## Data Model (JPA entities)

```java
@Entity
public class User {
    @Id @GeneratedValue Long id;
    @Column(unique = true) String phoneNumber;
    String passwordHash;
    String name;
    @Enumerated(EnumType.STRING) Role role; // FARMER, EXPERT, ADMIN, FIELD_WORKER (reserved, unused)
    String district, upazila;
    Instant createdAt;
}

@Entity
public class Crop {
    @Id @GeneratedValue Long id;
    String nameBn, nameEn;
    boolean isActive; // seed only "Rice" for now
}

@Entity
public class Disease {
    @Id @GeneratedValue Long id;
    @ManyToOne Crop crop;
    String nameBn, nameEn;
    String modelClassLabel; // EXACT Bangla string the AI service returns — match by direct string equality
    boolean isActive;
}

@Entity
public class Treatment {
    @Id @GeneratedValue Long id;
    @ManyToOne Disease disease;
    @Lob String textBn;
    boolean isOrganicPriority;
    String sourceNote, verifiedBy;
    boolean isActive;
    // NEVER write to this entity from the prediction flow — admin CRUD only
}

@Entity
public class Diagnosis {
    @Id @GeneratedValue Long id;
    @ManyToOne User farmer;
    @ManyToOne Crop crop;
    @Enumerated(EnumType.STRING) InputType inputType; // IMAGE, TEXT, VOICE
    String inputText; // nullable
    String imagePath; // nullable
    @ManyToOne Disease disease; // nullable if unmatched
    String diseaseNameRaw;
    Double confidence;
    boolean needsExpertReview;
    String aiMessage; // the AI service's own "message" field, stored as-is
    @ManyToOne Treatment treatment; // nullable
    @Enumerated(EnumType.STRING) Outcome outcomeFeedback; // YES, NO, SOMEWHAT, nullable
    Instant createdAt;
}

@Entity
public class Conversation {
    @Id @GeneratedValue Long id;
    @ManyToOne User farmer;
    @ManyToOne User expert; // nullable until assigned
    @ManyToOne Diagnosis diagnosis; // nullable
    @Enumerated(EnumType.STRING) ConversationStatus status; // OPEN, RESOLVED
    Instant createdAt;
}

@Entity
public class Message {
    @Id @GeneratedValue Long id;
    @ManyToOne Conversation conversation;
    @ManyToOne User sender;
    String body; // nullable
    String imagePath; // nullable
    boolean isRead;
    Instant createdAt;
}
```

## Endpoints to build

### Auth
- `POST /auth/register` — `{phoneNumber, password, name, role?}` (role defaults `FARMER`; only an authenticated `ADMIN` can register `EXPERT`/`ADMIN` accounts)
- `POST /auth/login` — `{phoneNumber, password}` → `{accessToken, tokenType, expiresIn, user}`
- All other endpoints below require `Authorization: Bearer <token>`, except `GET /health`

### Prediction (the core of this task)
- `POST /predict/text` — body `{text, inputType?}` (`inputType`: `"typed"` default or `"voice"`, just metadata)
- `POST /predict/image` — `multipart/form-data`, field `image`
- Both endpoints delegate to `PredictionOrchestrator` (see the SOLID section below for the exact collaborator breakdown), which:
  1. Validates input (non-empty text; valid image type/size) **before** calling the AI service — this saves a wasted upstream call on obviously-bad input
  2. Calls the AI service via `AiPredictionClient`, attaching `X-API-Key`, with a request timeout (~10s) and a clear exception type on failure/timeout that maps to HTTP `502`
  3. Matches the returned `disease` string to a `Disease` entity via `DiseaseMatcher`. **If nothing matches** (the AI returned a string that doesn't correspond to any seeded `Disease` row — a real edge case, test for it explicitly), do **not** fail the request: proceed with `disease` FK left null, `diseaseNameRaw` storing the raw AI string, and `treatment` returned as `null`. The farmer should still get *something* back rather than an error, even if your DB doesn't have that disease mapped yet.
  4. Looks up the active `Treatment` via `TreatmentLookupService` (may be null if unmatched, or if no treatment has been added yet for a matched disease — that's fine, return `treatment: null` either way, and the app shows a "contact an expert" fallback)
  5. For image uploads, saves the file to local disk (e.g. `./data/images/{diagnosisId}_{originalFilename}` or a UUID-based name) and records the path as `Diagnosis.imagePath` — this build stores images on the local filesystem the backend runs on, not cloud storage
  6. Persists a `Diagnosis` row via `DiagnosisRecorder`
  7. Asks `EscalationPolicy` whether this case needs expert escalation; if yes, creates an open, unassigned `Conversation` linked to this diagnosis
  8. Returns `{diagnosisId, disease, confidence, needsExpertReview, message, treatment}` to the caller

### Diagnosis history
- `GET /diagnoses?limit=&offset=&farmerId=` — role-scoped (farmers see only their own; experts/admins may filter by `farmerId`)
- `GET /diagnoses/{id}` — `404` if not visible to the caller
- `POST /diagnoses/{id}/feedback` — body `{outcome: "yes"|"no"|"somewhat"}`

### Knowledge base (crops/diseases/treatments)
- `GET /crops`, `GET /diseases?cropId=`, `GET /treatments?diseaseId=` — open to any authenticated user
- `POST`/`PUT` on all three — `@PreAuthorize("hasRole('ADMIN')")` only

### Messaging
- `GET /conversations` — role-scoped (farmer: own; expert: assigned + unassigned-open queue; admin: all)
- `GET /conversations/{id}/messages` — marks the caller's unread messages as read; `404` if not a participant
- `POST /conversations/{id}/messages` — `multipart/form-data`, optional `body` text and/or `image` file (at least one required)
- `PATCH /conversations/{id}` — `{expertId}` to claim, or `{status: "resolved"}`; expert/admin only

### Health
- `GET /health` → `{"status": "ok", "service": "Fasol Doctor Backend (Spring Boot)"}` — no auth

## Error handling
Use a `@ControllerAdvice`/`GlobalExceptionHandler` so every error response — yours and anything mapped from the AI service — has this exact shape:
```json
{ "detail": "Human-readable message" }
```
| Status | Meaning |
|---|---|
| 400 | Invalid input (empty text, bad file type, validation failure) |
| 401 | Missing/invalid/expired JWT |
| 403 | Authenticated but role-forbidden |
| 404 | Resource not found or not visible to this caller |
| 422 | Request body failed `@Valid` schema validation |
| 502 | The upstream AI service failed or timed out |

## Config (`application.yml`)
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:file:./data/fasol_doctor
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: update

ai-service:
  base-url: http://10.100.59.183:8000
  api-key: ${AI_SERVICE_API_KEY}

jwt:
  secret: ${JWT_SECRET}
  expiration-minutes: 60

app:
  max-file-size-mb: 5
  image-storage-path: ./data/images   # local disk — not cloud storage, for this build
```
Never hardcode `AI_SERVICE_API_KEY` or `JWT_SECRET` — read them from environment variables. Only commit an `application.yml.example` with placeholder values.

## Seed data
On startup (via a `CommandLineRunner` or `data.sql`), create:
- One `Crop`: Rice
- The 6 `Disease` rows for Rice, using the exact Bangla `modelClassLabel` values from the table above
- One placeholder `Treatment` per disease — clearly flagged in a code comment as **not agronomist-verified, placeholder content only**

## Applying SOLID principles (concrete, not generic — tied to this codebase)

Generic SOLID advice tends to be ignored in practice. Here's exactly where and how to apply each principle in this project, and why it matters for *this* codebase specifically:

### Single Responsibility Principle
- **Don't build one fat `PredictionService`.** Split the prediction flow into focused collaborators, each with one reason to change:
  - `AiPredictionClient` — only talks HTTP to the external AI service
  - `DiseaseMatcher` — only matches an AI response string to a `Disease` entity
  - `TreatmentLookupService` — only fetches the active treatment for a disease
  - `DiagnosisRecorder` — only persists a `Diagnosis` row
  - `EscalationPolicy` — only decides whether a case needs expert escalation
  - `PredictionOrchestrator` — coordinates the above, in order, with no business logic of its own beyond sequencing
- Controllers do HTTP concerns only (parsing request, calling one service method, mapping the result to a response DTO) — no business logic in `@RestController` classes.
- Split `AdminController`/`AdminService` into separate `CropService`, `DiseaseService`, `TreatmentService` rather than one class handling all three resources.

### Open/Closed Principle
- **`DiseaseMatcher`**: today it's an exact string match (`Disease.modelClassLabel == aiResponse.disease`). Build it as an interface (`DiseaseMatcher`) with one implementation (`ExactMatchDiseaseMatcher`) — this means if matching logic later needs to get fuzzier (e.g. handling near-miss spelling), you add a new implementation without touching `PredictionOrchestrator`. **Contract:** returns `Optional<Disease>` (or nullable) rather than throwing when nothing matches — an unmatched disease string is an expected, non-exceptional outcome that `PredictionOrchestrator` must handle gracefully (see the Prediction section above), not an error condition.
- **`EscalationPolicy`**: today the rule is just "trust the AI's `needsExpertReview` flag." Behind an interface, this can later incorporate other signals (e.g. farmer's diagnosis history, repeated low-confidence results) without changing the orchestrator that calls it.
- **Notification on new message (MSG-07)**: define a `NotificationService` interface with one implementation (`InAppUnreadFlagNotificationService`) now. If push notifications (FCM) are added in a later phase, that's a new implementation, not a rewrite of `ConversationService`.

### Liskov Substitution Principle
- Every interface above must be fully substitutable — a test double (e.g. a fake `AiPredictionClient` that returns a canned response) must work anywhere the real implementation does, with no special-casing in calling code. This is what makes step 4 of the Build Order ("mock the AI service if unreachable") actually clean to do: swap the Spring bean, not the calling code.
- Don't create an implementation that throws `UnsupportedOperationException` on a method the interface promises to support — if a method doesn't make sense for an implementation, the interface is wrong, not the implementation.

### Interface Segregation Principle
- Don't create one giant `AdminOperations` interface with every CRUD method for crops, diseases, treatments, and users mixed together. Keep `CropRepository`, `DiseaseRepository`, `TreatmentRepository` as separate Spring Data JPA interfaces (this falls out naturally from one-repository-per-entity, but resist the urge to merge them into a shared "admin repository").
- Role-scoped read logic (farmer sees own diagnoses; expert sees assigned + queue; admin sees all) should live behind one `DiagnosisQueryService` method that takes the caller's role/id as a parameter — not three different interfaces per role, since the *shape* of what's returned is the same, only the filter differs.

### Dependency Inversion Principle
- Every service/controller depends on **interfaces**, injected via constructor (Spring's `@Service`/`@Component` + constructor injection handles this naturally — avoid field injection with `@Autowired` on fields, it hides the dependency graph and breaks easy substitution).
- `PredictionOrchestrator` depends on `AiPredictionClient` (interface), never directly on `RestClient`/`RestTemplate` — the HTTP client detail is an implementation concern of `AiPredictionClientImpl`.
- `AuthService` depends on a `PasswordHasher`-style interface wrapping `BCryptPasswordEncoder` if you want this to be swappable/testable in isolation (optional but consistent with the pattern above).

### Practical package layout reflecting this
```
service/
├── prediction/
│   ├── AiPredictionClient.java          (interface)
│   ├── AiPredictionClientImpl.java
│   ├── DiseaseMatcher.java              (interface)
│   ├── ExactMatchDiseaseMatcher.java
│   ├── EscalationPolicy.java            (interface)
│   ├── DefaultEscalationPolicy.java
│   └── PredictionOrchestrator.java      (coordinates the above)
├── notification/
│   ├── NotificationService.java         (interface)
│   └── InAppUnreadFlagNotificationService.java
├── CropService.java, DiseaseService.java, TreatmentService.java
├── DiagnosisService.java
├── ConversationService.java
└── AuthService.java
```

**Don't over-apply this.** Simple CRUD services (`CropService`, `DiseaseService`) don't need interfaces if there's realistically only ever going to be one implementation — introducing an interface with exactly one implementation and no planned second one is unnecessary indirection, not good design. Reserve interfaces for the points above that are *actually* likely to vary (the prediction pipeline's individual steps, notification delivery) — that's where SOLID earns its cost.


1. Project skeleton boots, `GET /health` responds
2. Entities + seed data, verify via H2 console that the 6 diseases and treatments exist
3. Auth (register/login/JWT), verify with `curl`
4. Prediction gateway (`/predict/text`, `/predict/image`) — build the `PredictionOrchestrator` and its collaborators (`AiPredictionClient`, `DiseaseMatcher`, `TreatmentLookupService`, `DiagnosisRecorder`, `EscalationPolicy`) per the SOLID section above. Verify against the real AI service if reachable; if not reachable, substitute a fake `AiPredictionClient` implementation and clearly state that's what you did. **Test at least three cases explicitly**, not just the happy path: (a) a valid image with a clear result, (b) a corrupted/non-image file — confirm this gets rejected by your own validation *before* any call to the AI service, (c) if possible, a genuinely blurry image — confirm the AI service's own `400` "ছবিটি অস্পষ্ট" error passes through unchanged rather than being swallowed or rewritten
5. Diagnosis history + feedback
6. Crop/disease/treatment CRUD (admin-gated)
7. Messaging (conversations + messages)

Verify each step works before moving to the next. State briefly what you tested and observed at each step.

## Non-goals for this task
- No frontend of any kind
- No SMS/USSD, no WhatsApp, no push notifications
- No database migration tooling (Flyway/Liquibase) — `ddl-auto: update` is fine for now
- No production security hardening (HTTPS termination, secrets manager, refresh tokens) — local JWT auth is sufficient
- No regional dashboards or analytics — this is Phase 3 scope, not relevant here
