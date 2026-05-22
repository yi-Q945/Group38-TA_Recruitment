# RecruitAssist — Feature Guide & User Manual

> **Version**: v4.0.0 + Sprint 5 Deployment Addendum  
> **Date**: May 2026  
> **Team**: Group 38, EBU6304 Software Engineering

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Technology Stack](#2-technology-stack)
3. [Getting Started](#3-getting-started)
4. [Feature Reference](#4-feature-reference)
   - 4.1 [Authentication & Session Management](#41-authentication--session-management)
   - 4.2 [TA Features](#42-ta-features)
   - 4.3 [MO Features](#43-mo-features)
   - 4.4 [Admin Features](#44-admin-features)
   - 4.5 [Recommendation Engine](#45-recommendation-engine)
5. [Data Architecture](#5-data-architecture)
6. [Configuration](#6-configuration)
7. [API Endpoints](#7-api-endpoints)

---

## 1. System Overview

**RecruitAssist** is a lightweight Java Servlet/JSP prototype system designed for the **Teaching Assistant (TA) recruitment workflow**. It supports three user roles — TA, Module Organiser (MO), and Admin — each with dedicated dashboards and tailored functionality.

### Core Highlights

- **Explainable Recommendation Engine**: 6-dimensional weighted scoring model that provides human-readable match explanations
- **Complete Application Lifecycle**: Submit → Shortlist → Accept/Reject → Withdraw, with automatic job closure
- **Role-Based Access Control**: Three roles with distinct views, actions, and data visibility
- **Zero-Database Architecture**: All data stored as JSON/CSV/TXT files with built-in caching and concurrency control
- **Production-Ready Input Validation**: XSS prevention, file type whitelist, quota consistency enforcement

### System Statistics (v4.0.0)

| Metric | Value |
|--------|-------|
| Java Classes | 50 |
| JSP Pages | 8 |
| Servlets | 19 (1 abstract base + 18 concrete) |
| Services | 8 |
| Repositories | 7 |
| Demo Users | 100+ |
| Demo Jobs | 50+ |
| Demo Applications | 500+ |

### Sprint 5: Server Deployment & Production Readiness (Haopeng Jin)

Sprint 5 is treated as a deployment and production-readiness extension after Sprint 4. It is mainly owned by **Haopeng Jin**. The goal is not to add a new business role, but to make RecruitAssist runnable on an external server and to turn PDF sharing, concurrency protection, and deployment verification into demonstrable engineering work.

- **Public deployment URL**: `http://RecruitAssistGroup38.21.214.216.122.nip.io:8080/home`
- **Server command record**: `homework/plan/服务器端指令.md`
- **Deployment package directory**: `homework/releases/server/RecruitAssist-server-v4.0.0/`
- **Deployment archive**: `homework/releases/server/RecruitAssist-server-v4.0.0.tar.gz`
- **Environment requirements**: Java 17, Maven, Jetty 12 EE10, port 8080, writable `data/` and `logs/`
- **Startup script**: `scripts/start-maven-jetty.sh` sets `RECRUITASSIST_BASE_DIR` automatically so the server reads the packaged `data/`
- **Health check**: `/health` returns JSON status and confirms that users, jobs and applications are loaded
- **Data integrity check**: `DATA_MANIFEST.json` is included in the package to verify `users/jobs/applications/cv/system` file counts and hashes

Sprint 5 also adds two production-readiness features:

- **Permanent protected PDF token links**: `PdfShareService.java` assigns each CV owner a stable random token; `SharedPdfServlet.java` serves `/pdf/share?token=...` inline only after login and TA self-access, MO job ownership, or Admin role checks.
- **Concurrency and availability extension**: Sprint 4's CSRF protection, POST logout, synchronized registration, cross-process file locks, atomic writes, corrupted JSON isolation and `scripts/load_test_recruitassist.py` are carried into the server deployment and validated against the Java 17 / Jetty 12 runtime.

### Feature Ownership Reference

For presentation and marking, each feature area can be traced to an owner, code path, and visible demo example:

| Owner | Feature Area | Code Paths | User-Facing Example |
|-------|--------------|------------|---------------------|
| Yi Qi | Login, registration, homepage, session entry UI | `LoginServlet.java`, `RegisterServlet.java`, `LogoutServlet.java`, `HomeServlet.java`, `login.jsp`, `register.jsp`, `home.jsp` | A visitor opens `/home`, sees live stats, uses quick sign-in or registers a TA/MO account, then enters the correct dashboard. |
| Tianyu Zhao | TA dashboard, profile, CV upload/download, apply/withdraw flow | `UpdateProfileServlet.java`, `UploadCvServlet.java`, `DownloadCvServlet.java`, `ApplyServlet.java`, `WithdrawApplicationServlet.java`, `dashboard-ta.jsp` | A TA updates skills, uploads a CV, applies for a recommended job, and sees the application in history. |
| Jie Ren | MO dashboard, job CRUD, candidate management | `CreateJobServlet.java`, `UpdateJobServlet.java`, `ChangeJobStatusServlet.java`, `JobService.java`, `dashboard-mo.jsp`, `job-detail.jsp` | An MO creates a job, edits requirements, reviews ranked candidates, and changes an application to Shortlisted/Accepted/Rejected. |
| Haopeng Jin | Recommendation engine, application lifecycle, Sprint 5 deployment, permanent PDF token sharing, security/concurrency and availability hardening | `RecommendationService.java`, `ApplicationService.java`, `AppServlet.java`, `JsonFileStore.java`, `IdCounterRepository.java`, `PdfShareService.java`, `SharedPdfServlet.java`, `scripts/load_test_recruitassist.py`, `homework/releases/server/`, `homework/plan/服务器端指令.md` | The system explains a TA-job match, prevents duplicate applications, validates CSRF tokens, exposes `/health`, supports load testing, and is deployed on the public server. |
| Zhuang Hou | Admin dashboard, workload monitoring, CSV export | `DashboardServlet.renderAdminDashboard()`, `WorkloadService.java`, `AdminExportServlet.java`, `dashboard-admin.jsp` | Admin checks TA workload risk and exports jobs/applications/workload CSV reports. |
| Zexuan Dong | File-backed infrastructure, repositories, bootstrap and tests | `AppPaths.java`, `AppServices.java`, `JsonFileStore.java`, repositories, `AppBootstrapListener.java`, `src/test/java/**` | The project runs without a database: JSON/CSV/TXT files store users, jobs, applications, notifications, config and audit logs. |

---

## 2. Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 17 |
| Build Tool | Maven | 3.9+ |
| Web Framework | Jakarta Servlet | 6.0.0 |
| View Layer | JSP + JSTL | 3.0 |
| JSON Processing | Gson | 2.10.1 |
| Application Server | Jetty (embedded) | 12.0.15 |
| Packaging | WAR | - |
| Storage | JSON / CSV / TXT files | - |
| Testing | JUnit Jupiter | 5.10.2 |

---

## 3. Getting Started

### Prerequisites

- Java 17 (JDK)
- Maven 3.9+
- Python 3 (optional, for demo data generation)

### Quick Start

```bash
# Clone
git clone https://github.com/yi-Q945/Group38-TA_Recruitment.git
cd Group38-TA_Recruitment

# (Optional) Generate demo data
python3 scripts/generate_demo_load.py

# Start
export RECRUITASSIST_BASE_DIR=$(pwd)
mvn -f framework/recruitassist-web/pom.xml \
    org.eclipse.jetty.ee10:jetty-ee10-maven-plugin:12.0.15:run \
    -Djetty.http.port=8081 -Djetty.contextPath=/
```

Open http://127.0.0.1:8081/ in your browser.

### Demo Accounts

| Role | Username | Password |
|------|----------|----------|
| TA | `alice.ta` | `demo123` |
| TA | `ben.ta` | `demo123` |
| MO | `mo.chen` | `demo123` |
| MO | `recruiter.01` | `demo123` |
| Admin | `admin.sarah` | `demo123` |

---

## 4. Feature Reference

### 4.1 Authentication & Session Management

**Login** (`/login`)
- Username + password form-based authentication
- Session-based state management with `HttpSession`
- **Session fixation protection**: after a successful login, the old session is invalidated and a fresh session is created before storing the authenticated user ID
- **Username enumeration protection**: failed login attempts always show the same "Invalid username or password" message
- **Password hashing**: newly registered passwords are stored with PBKDF2-HMAC-SHA256; legacy demo plaintext passwords remain readable for backwards-compatible seed data
- **Failed login throttling**: five consecutive failed attempts for the same username key trigger a temporary 5-minute lockout
- **CSRF protection**: all POST forms include a session token and `AppServlet` rejects invalid or missing tokens with HTTP 403
- **Session hardening**: authenticated sessions expire after 30 minutes of inactivity and all Servlet responses include baseline security headers (`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Cache-Control`)
- Flash message system for success/error feedback (for example, "Signed in as Amelia Chen.")
- **Demo user quick-select panel**: the login page shows up to 3 TA accounts, 2 MO accounts, and 1 Admin account, and the "Use account" button auto-fills the form
- **Live account statistics**: total users, TA count, MO count, and Admin count are displayed in the login page header
- **Sticky form state**: failed login attempts keep the submitted username
- Authenticated users visiting `/login` are redirected to `/dashboard`

**Registration** (`/register`)
- New account form with 6 fields: username, display name, email, role (TA or MO), password, and confirm password
- **Multi-layer validation**:

| Check | Layer | Implementation |
|-------|-------|----------------|
| Username format | Client + Server | HTML5 `pattern="[a-zA-Z0-9._-]{3,30}"` + Java regex |
| Username uniqueness | Server | `findByUsername()` lookup |
| Display name uniqueness | Server | Case-insensitive scan of existing user display names |
| Password length | Client + Server | HTML5 `minlength=6` + Java length check |
| Password confirmation | Server | `password.equals(confirmPassword)`, then PBKDF2 hash before persistence |
| Admin role block | Server | `role == ADMIN` is rejected |
| Email format | Client + Server | HTML5 `type=email` + Java regex |
| XSS prevention | Server | `cleanText()` strips angle brackets and control characters |

- **Admin self-registration is blocked**: only TA and MO roles are exposed in the form, and tampered `role=ADMIN` requests are rejected server-side
- **Sticky form fields**: validation failures preserve username, display name, email, and selected role
- Successful registration sets a flash message and redirects the user to `/login`
- `UserService.registerUser()` is synchronized so username uniqueness checks and user creation remain atomic under concurrent registration attempts

**Logout** (`/logout`)
- Invalidates the current session completely
- Creates a new session with "You have been signed out." flash message
- Uses POST + CSRF token instead of GET, then redirects to the login page

**Home Page** (`/home`)
- Public landing page for unauthenticated users with a modern hero layout
- Displays 5 live system statistics: total TAs, MOs, Admins, jobs, and applications
- Provides one-click Quick Sign-In cards for representative TA, MO, and Admin accounts
- Shows feature cards for the TA, MO, and Admin workflows
- Includes a suggested 3-step demo path for presentations
- Authenticated users are automatically redirected to their dashboard

**Health Check** (`/health`)
- Public readiness endpoint for Sprint 4 deployment checks
- Verifies required directories are present and returns live user/job/application counts
- Returns HTTP 200 with `{"status":"UP"}` when the file-backed application is readable, or HTTP 503 with `{"status":"DOWN"}` when a runtime storage check fails

### 4.2 TA Features

#### 4.2.1 Dashboard (`/dashboard` — TA view)

The TA Dashboard provides a comprehensive workspace:

- **Hero Section**: Welcome message + KPI cards (workload, active applications, profile completeness)
- **Top Recommendation Spotlight**: Highlights the best-matching job with score and fit label
- **Profile Management Form**: Edit name, student ID, email, programme, skills, availability, experience, CV text
- **CV Upload**: Support for PDF, DOC, DOCX, TXT (max 5MB)
- **Application History**: Table showing all applications with status, score, and timestamp
- **Application Notifications**: Shows status-change notifications from MOs with unread count and "mark as read" action
- **Recommended Jobs Grid**: Searchable/sortable list of job cards with:
  - Overall match percentage and fit label
  - Matched/missing skill tags
  - 6-dimension progress bars (skill, availability, experience, workload, profile, competition)
  - Human-readable explanation reasons
  - Apply/View Detail buttons

**Search & Sort**: Filter by keywords (title, module, skills, description). Sort by recommendation score, deadline, or workload impact.

#### 4.2.2 Apply for Position (`/apply`)

- One-click application from dashboard or job detail page
- Pre-submission validation:
  - Profile must be sufficiently complete (name, email, skills required)
  - Job must be OPEN and not expired
  - Quota must not be full
  - No duplicate application (existing non-withdrawn application blocks re-apply)
- Recommendation score is computed and stored with the application
- Audit log entry is created

#### 4.2.3 Withdraw Application (`/applications/withdraw`)

- Available for applications in SUBMITTED or SHORTLISTED status
- Sets status to WITHDRAWN
- Audit log entry is created
- User can re-apply after withdrawal

#### 4.2.4 Application Notifications (`/notifications/read`)

- Created automatically when an MO changes an application to SHORTLISTED, ACCEPTED, or REJECTED
- TA dashboard shows recent notifications and unread count
- TA can mark notifications as read; only the notification recipient can update read state

#### 4.2.5 Profile Management (`/profile/update`)

Editable fields:
- Name, Student ID, Email (validated format)
- Programme, Skills (comma/semicolon/newline separated)
- Availability text, Experience text, CV text
- Input sanitisation: strips HTML tags, control characters, enforces max length

#### 4.2.6 CV Upload (`/profile/cv/upload`)

- Supported formats: PDF, DOC, DOCX, TXT
- Max file size: 5MB
- Stored as `{userId}_cv.{extension}` in `data/cv/`
- Old CV files are automatically deleted on re-upload
- Metadata (filename, upload timestamp) saved to user profile
- Each CV owner receives a permanent random `pdfShareToken` stored in `UserProfile`
- The token link is shown as `/pdf/share?token=...`; TAs can open their own link, MOs can open candidate links only for their own jobs, and Admin can open any valid CV link
- `SharedPdfServlet.java` proxies the file response with role checks instead of exposing `data/cv/` as a public static directory

#### 4.2.7 Permanent PDF Link Design (`/pdf/share`)

The PDF link is permanent at the user level, not temporary. It is still protected because the token alone is not enough:

- **Token owner**: `UserProfile.pdfShareToken`, generated by `PdfShareService.ensureToken()`
- **Access path**: `GET /pdf/share?token={token}` for TA/Admin, or `GET /pdf/share?token={token}&jobId={jobId}` for MO candidate review
- **TA rule**: can open only their own CV link
- **MO rule**: can open a candidate link only when the candidate applied to a job owned by that MO
- **Admin rule**: can open any CV link for audit and review
- **Storage rule**: the real file remains under `data/cv/`; the Servlet normalizes the path and prevents direct path traversal
- **UX example**: TA sees "Open permanent PDF link" in Profile & CV management; MO sees "Open PDF" in candidate queues; Admin sees "Open PDF" in latest applications

### 4.3 MO Features

#### 4.3.1 Dashboard (`/dashboard` — MO view)

- **KPI Cards**: Open jobs, total applications, shortlisted, accepted counts
- **Job Overview Cards**: Each owned job displayed with module, title, status, deadline, quota, workload hours
- **Create New Job Form**: Title, module code, deadline, quota, workload hours, required skills, preferred skills, description
- **Candidate Queues**: Per-job tables showing applicants with:
  - Name, skills, workload (current hours)
  - Recommendation score percentage + explanation summary
  - Status dropdown for accept/reject/shortlist
  - Open/Close job toggle button

#### 4.3.2 Create Job (`/jobs/create`)

Validated fields:
- Title (required, max 200 chars)
- Module code (required)
- Deadline (must be future date, `yyyy-MM-dd` format)
- Quota (positive integer)
- Workload hours per week (positive integer)
- Required skills (at least 1)
- Preferred skills (optional)
- Description (required)

XSS prevention: all text fields are cleaned of `<>` tags and control characters.

#### 4.3.3 Edit Job (`/jobs/update`)

- Only the job owner can edit
- Quota cannot be reduced below the number of already-accepted applications
- If deadline passes or quota fills after edit, job automatically closes

#### 4.3.4 Close / Reopen Job (`/jobs/status`)

- **Close**: Sets job status to CLOSED
- **Reopen**: Sets job status back to OPEN
  - Validates deadline hasn't passed
  - Validates quota isn't already full
- Audit log entries for all status changes

#### 4.3.5 Review Applications (`/applications/status`)

Status transitions available to MO:
- SUBMITTED → SHORTLISTED
- SUBMITTED → ACCEPTED
- SUBMITTED → REJECTED
- SHORTLISTED → ACCEPTED
- SHORTLISTED → REJECTED

**Auto-close**: When the last available quota slot is filled by an ACCEPTED application, the job automatically closes.

**Notifications**: Successful status changes create a TA-facing notification so applicants can see shortlist/accept/reject outcomes without manually opening every application.

#### 4.3.6 Open Candidate PDF (`/pdf/share`)

Access control:
- MO can only open CV/PDF links for applicants who applied to their jobs
- Admin can open any CV/PDF link
- TA can open their own CV/PDF link
- `/pdf/share` returns the file inline for browser preview; `/cv/download` remains available for attachment download

### 4.4 Admin Features

#### 4.4.1 Dashboard (`/dashboard` — Admin view)

- **KPI Cards**: Tracked TAs, recent applications, policy threshold, filtered jobs
- **Recruitment Overview Table**: All jobs filterable by status (Open/Closed) and module code search
  - Columns: Job, Owner, Deadline, Applicants, Accepted/Quota, Status
- **TA Workload Table**: All TAs with:
  - Accepted hours, active applications
  - "Balanced" / "Over threshold" indicator
- **Recent Applications**: Latest 10 applications across the system
- **CSV Export**: Download jobs, applications, or workload reports as dated CSV files

#### 4.4.2 CSV Export (`/admin/export`)

- Admin-only endpoint
- Export types:
  - `type=jobs`: job ID, module, title, owner, deadline, status, quota, workload, application count, accepted count
  - `type=applications`: application ID, job, module, applicant, status, recommendation percentage, submitted time
  - `type=workload`: TA user ID, name, programme, accepted hours, active applications, threshold, status
- Files are returned with `Content-Disposition: attachment` and date-stamped names

### 4.5 Recommendation Engine

The recommendation engine is the core innovation of RecruitAssist. It evaluates each TA-Job pair across **6 weighted dimensions**:

#### Scoring Formula

```
final_score = Σ(dimension_score × weight) / Σ(weight)
```

#### Dimensions

| Dimension | Weight |
|-----------|--------|
| **Skill Match** | 40% |
| **Experience** | 18% |
| **Availability** | 12% |
| **Workload Balance** | 12% |
| **Profile Evidence** | 10% |
| **Competition** | 8% |

**Skill Match (40%)**
Required coverage ×0.72 + preferred ×0.18 + breadth bonus + full coverage bonus − gap penalty.
Uses Jaccard similarity ≥0.55 for fuzzy matching and skill alias normalization.

**Experience (18%)**
Base 0.3 + text evidence bonuses. Analyses phrase coverage (job keywords in profile),
token overlap, and evidence keyword hits (lab, marking, debugging, etc.).

**Availability (12%)**
Base 0.34 + keyword bonuses for weekday/weekend, specific days, time slots, flexibility indicators.

**Workload Balance (12%)**
If under threshold: 0.55 + remaining ratio ×0.45. If over: penalty proportional to overshoot.
Promotes fair distribution.

**Profile Evidence (10%)**
Completeness of 5 profile fields (58%) + job alignment (22%) + breadth signal (20%).

**Competition (8%)**
Active applicants per remaining slot. ≤1/slot → 0.95; ≤2 → 0.82; ≤3 → 0.68;
progressively lower for higher competition.

#### Explainability

Each dimension generates a natural-language explanation string. These reasons are displayed on the TA dashboard and job detail page, helping TAs understand *why* a job is recommended and *what* they can improve in their profile.

#### Skill Matching Details

- **Three-level matching**: (1) Canonical exact match → (2) Jaccard similarity ≥ 0.55 → (3) Token containment
- **Alias normalization**: "OOP" ↔ "Object Oriented Programming", "JS" ↔ "JavaScript", etc. (8 alias groups)
- **Tokenization**: Regex `[a-z0-9]{2,}` with stop-word removal

---

## 5. Data Architecture

### Storage Structure

```
data/
├── users/          # UserProfile JSON files (U*.json)
├── jobs/           # JobPosting JSON files (J*.json)
├── applications/   # ApplicationRecord JSON files (A*.json)
├── notifications/  # TA notification JSON files (N*.json)
├── cv/             # Uploaded CV files ({userId}_cv.{ext})
└── system/
    ├── config.json       # System configuration (weights, thresholds)
    └── id-counters.json  # Auto-increment ID counters
logs/
└── access/
    └── audit.csv   # Audit trail (action, userId, timestamp)
```

### Caching & Concurrency

**JsonFileStore** provides:
- **Two-level cache**: File-level cache (keyed by path + modifiedAt + size) and directory-level cache
- **Path-level read-write locks**: `ConcurrentHashMap<Path, ReentrantReadWriteLock>` for fine-grained concurrency
- **Atomic writes**: Data written to temp file first, then atomically moved to target path
- **Cross-process write locks**: writes acquire a sibling `.lock` file through `FileChannel.lock()` so two Tomcat instances do not write the same JSON/CSV file at the same time
- **Failure cleanup and resilience**: failed writes delete temporary files on a best-effort basis; directory reads skip corrupted JSON files instead of failing the whole listing
- **Cache invalidation**: Automatic on write operations; directory cache invalidated by prefix matching

### Sprint 4 Concurrency Evidence

- `ApplicationService` serialises application submission and status updates with a write lock, so duplicate active applications are checked and written as one critical section.
- `IdCounterRepository` uses an in-JVM lock and a cross-process file lock around counter read/update/write, preventing duplicated IDs under concurrent creation.
- `ApplicationConcurrencyTest` launches 24 simultaneous submissions for the same TA/job pair and verifies that exactly one succeeds and exactly one JSON application record is persisted.
- `PasswordHasherTest` and `AuthServiceTest` cover PBKDF2 verification, legacy demo password compatibility, and temporary lockout after repeated login failures.
- `NotificationServiceTest` verifies that MO status updates create unread TA notifications and that read-state updates are owner-only.
- JaCoCo coverage is generated during `mvn verify` under `framework/recruitassist-web/target/site/jacoco/`.

---

## 6. Configuration

### System Configuration (`data/system/config.json`)

```json
{
  "appName": "RecruitAssist",
  "storage": {
    "mode": "text-files-only",
    "formats": ["json", "csv", "txt"]
  },
  "workload": {
    "defaultMaxHours": 12
  },
  "recommendation": {
    "skillMatchWeight": 0.4,
    "availabilityWeight": 0.12,
    "experienceWeight": 0.18,
    "workloadBalanceWeight": 0.12,
    "profileEvidenceWeight": 0.1,
    "competitionWeight": 0.08
  }
}
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `RECRUITASSIST_BASE_DIR` | Project root directory | Auto-detected from CWD |
| `recruitassist.baseDir` | Same as above (Java system property) | - |

---

## 7. API Endpoints

| Method | URL | Role | Description |
|--------|-----|------|-------------|
| GET | `/home` | Public | Landing page with system stats |
| GET | `/health` | Public | Readiness/health check for deployment |
| GET/POST | `/login` | Public | Login form / authenticate |
| GET/POST | `/register` | Public | Registration form / create TA or MO account |
| POST | `/logout` | Authenticated | End session with CSRF token |
| GET | `/dashboard` | Authenticated | Role-specific dashboard |
| GET | `/jobs/detail?id={jobId}` | Authenticated | Job detail with role-specific view |
| POST | `/jobs/create` | MO | Create new job posting |
| POST | `/jobs/update` | MO | Edit existing job |
| POST | `/jobs/status` | MO | Close/reopen job |
| POST | `/apply` | TA | Submit application |
| POST | `/applications/withdraw` | TA | Withdraw application |
| POST | `/applications/status` | MO | Accept/reject/shortlist application |
| POST | `/notifications/read` | TA | Mark notification as read |
| GET | `/admin/export?type={jobs|applications|workload}` | Admin | Download CSV report |
| POST | `/profile/update` | TA | Update personal profile |
| POST | `/profile/cv/upload` | TA | Upload CV file |
| GET | `/cv/download?userId={id}` | Auth+ACL | Download CV file |
| GET | `/pdf/share?token={token}` | Auth+ACL | Open permanent protected CV/PDF link |
