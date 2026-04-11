# RecruitAssist — Feature Guide & User Manual

> **Version**: v3.0.0 (Sprint 3 Complete)  
> **Date**: April 2026  
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

### System Statistics (v3.0.0)

| Metric | Value |
|--------|-------|
| Java Classes | 42 |
| JSP Pages | 7 |
| Servlets | 14 (1 abstract base + 13 concrete) |
| Services | 6 |
| Repositories | 6 |
| Demo Users | 100+ |
| Demo Jobs | 50+ |
| Demo Applications | 500+ |

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
- Flash message system for success/error feedback
- Demo user quick-select panel on login page
- Session invalidation on logout for security

**Logout** (`/logout`)
- Invalidates current session
- Creates new session with "Logged out" flash message
- Redirects to login page

**Home Page** (`/home`)
- Public landing page for unauthenticated users
- Displays system statistics: total TAs, MOs, Admins, jobs, applications
- Authenticated users are automatically redirected to their dashboard

### 4.2 TA Features

#### 4.2.1 Dashboard (`/dashboard` — TA view)

The TA Dashboard provides a comprehensive workspace:

- **Hero Section**: Welcome message + KPI cards (workload, active applications, profile completeness)
- **Top Recommendation Spotlight**: Highlights the best-matching job with score and fit label
- **Profile Management Form**: Edit name, student ID, email, programme, skills, availability, experience, CV text
- **CV Upload**: Support for PDF, DOC, DOCX, TXT (max 5MB)
- **Application History**: Table showing all applications with status, score, and timestamp
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

#### 4.2.4 Profile Management (`/profile/update`)

Editable fields:
- Name, Student ID, Email (validated format)
- Programme, Skills (comma/semicolon/newline separated)
- Availability text, Experience text, CV text
- Input sanitisation: strips HTML tags, control characters, enforces max length

#### 4.2.5 CV Upload (`/profile/cv/upload`)

- Supported formats: PDF, DOC, DOCX, TXT
- Max file size: 5MB
- Stored as `{userId}_cv.{extension}` in `data/cv/`
- Old CV files are automatically deleted on re-upload
- Metadata (filename, upload timestamp) saved to user profile

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

#### 4.3.6 Download CV (`/cv/download`)

Access control:
- MO can only download CVs of applicants who applied to their jobs
- Admin can download any CV
- TA can download their own CV
- Returns file with `Content-Disposition: attachment`

### 4.4 Admin Features

#### 4.4.1 Dashboard (`/dashboard` — Admin view)

- **KPI Cards**: Tracked TAs, recent applications, policy threshold, filtered jobs
- **Recruitment Overview Table**: All jobs filterable by status (Open/Closed) and module code search
  - Columns: Job, Owner, Deadline, Applicants, Accepted/Quota, Status
- **TA Workload Table**: All TAs with:
  - Accepted hours, active applications
  - "Balanced" / "Over threshold" indicator
- **Recent Applications**: Latest 10 applications across the system

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
- **Cache invalidation**: Automatic on write operations; directory cache invalidated by prefix matching

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
| GET/POST | `/login` | Public | Login form / authenticate |
| GET | `/logout` | Authenticated | End session |
| GET | `/dashboard` | Authenticated | Role-specific dashboard |
| GET | `/jobs/detail?id={jobId}` | Authenticated | Job detail with role-specific view |
| POST | `/jobs/create` | MO | Create new job posting |
| POST | `/jobs/update` | MO | Edit existing job |
| POST | `/jobs/status` | MO | Close/reopen job |
| POST | `/apply` | TA | Submit application |
| POST | `/applications/withdraw` | TA | Withdraw application |
| POST | `/applications/status` | MO | Accept/reject/shortlist application |
| POST | `/profile/update` | TA | Update personal profile |
| POST | `/profile/cv/upload` | TA | Upload CV file |
| GET | `/cv/download?userId={id}` | Auth+ACL | Download CV file |
