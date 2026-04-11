# RecruitAssist — Team Contribution & Implementation Report

> **Version**: v3.0.0 (Sprint 3)  
> **Date**: April 2026  
> **Course**: EBU6304 Software Engineering — Group 38

---

## Table of Contents

1. [Team Overview](#1-team-overview)
2. [Member Contributions](#2-member-contributions)
   - 2.1 [Yi Qi — Login & Homepage & UI Assets](#21-yi-qi--login--homepage--ui-assets)
   - 2.2 [Tianyu Zhao — TA Dashboard & Application & CV](#22-tianyu-zhao--ta-dashboard--application--cv)
   - 2.3 [Jie Ren — MO Dashboard & Job Management](#23-jie-ren--mo-dashboard--job-management)
   - 2.4 [Haopeng Jin — Recommendation Engine & Application Service](#24-haopeng-jin--recommendation-engine--application-service)
   - 2.5 [Zhuang Hou — Admin Dashboard & Workload Monitoring](#25-zhuang-hou--admin-dashboard--workload-monitoring)
   - 2.6 [Zexuan Dong — Data Layer & Infrastructure & Testing](#26-zexuan-dong--data-layer--infrastructure--testing)
3. [Cross-Cutting Concerns](#3-cross-cutting-concerns)
4. [Product Backlog Coverage](#4-product-backlog-coverage)

---

## 1. Team Overview

| Member | Primary Responsibility | Key Files | Lines of Code (approx.) |
|--------|----------------------|-----------|------------------------|
| Yi Qi | Login & Homepage & UI | 7 files | ~500 |
| Tianyu Zhao | TA Dashboard & CV & Apply | 8 files | ~700 |
| Jie Ren | MO Dashboard & Job CRUD | 6 files | ~700 |
| Haopeng Jin | Recommendation Engine & Application | 7 files | ~1,500 |
| Zhuang Hou | Admin Dashboard & Workload | 6 files | ~500 |
| Zexuan Dong | Data Layer & Infrastructure | 15 files | ~1,200 |

---

## 2. Member Contributions

### 2.1 Yi Qi — Login & Homepage & UI Assets

**Responsible Files**:
- `LoginServlet.java`, `LogoutServlet.java`, `HomeServlet.java`
- `login.jsp`, `home.jsp`, `index.jsp`
- `README.md`, `README_zh.md`, `figure/*.png`

**Backlog Stories**: #1 User Login & Role-Based Access, #13 Responsive UI

#### Feature Description (for Demo Presentation)

**Login Page** (`/login`)
- Username + password form-based authentication with session management
- A **Demo User Quick-Select Panel** is displayed on the login page, showing up to 3 TA accounts, 2 MO accounts, and 1 Admin account for easy demo access
- On successful login, the old session is invalidated and a new one is created (prevents session fixation attacks)
- Flash message system displays success/error feedback (e.g., "Welcome back, Alice!")
- Failed login shows a generic "Invalid username or password" error without revealing which field was wrong

**Logout** (`/logout`)
- Invalidates the current session, creates a new session with "Logged out" flash message, and redirects to the login page

**Home Page** (`/home`)
- Public landing page for unauthenticated users
- Displays real-time system statistics: total number of TAs, MOs, Admins, job postings, and applications
- Authenticated users are automatically redirected to their role-specific dashboard

**Demo Walkthrough**: Open the app → you see the Home page with system stats → click Login → use the quick-select panel to pick `alice.ta` → enter password `demo123` → you are redirected to the TA Dashboard with a welcome flash message.

#### Implementation Details

**1. Login Authentication Flow** (`LoginServlet.java`)

The login process implements session-based authentication with security best practices:

```java
// LoginServlet.java — doPost() (lines 31-53)
@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
    String username = req.getParameter("username");
    String password = req.getParameter("password");

    Optional<UserProfile> userOpt = services(req).authService()
            .authenticate(username, password);

    if (userOpt.isEmpty()) {
        req.setAttribute("loginError", "Invalid username or password.");
        populateLoginView(req);
        req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
        return;
    }

    // Security: invalidate old session to prevent session fixation
    req.getSession().invalidate();
    HttpSession session = req.getSession(true);
    session.setAttribute("userId", userOpt.get().getUserId());
    setFlash(req, "success", "Welcome back, " + userOpt.get().getName() + "!");
    redirect(req, resp, "/dashboard");
}
```

Key security measures:
- Session invalidation before creating new session (prevents session fixation attacks)
- Flash message system for user feedback
- Failed login shows error without revealing whether username or password was wrong

**2. Demo User Quick-Select Panel** (`LoginServlet.java`)

```java
// LoginServlet.java — populateLoginView() (lines 55-68)
private void populateLoginView(HttpServletRequest req) {
    List<UserProfile> allUsers = services(req).userService().listAllUsers();
    req.setAttribute("demoTAs", allUsers.stream()
            .filter(u -> u.getRole() == UserRole.TA).limit(3).toList());
    req.setAttribute("demoMOs", allUsers.stream()
            .filter(u -> u.getRole() == UserRole.MO).limit(2).toList());
    req.setAttribute("demoAdmins", allUsers.stream()
            .filter(u -> u.getRole() == UserRole.ADMIN).limit(1).toList());
}
```

**3. Home Page System Statistics** (`HomeServlet.java`)

```java
// HomeServlet.java — doGet() (lines 22-53)
long taCount = allUsers.stream()
        .filter(u -> u.getRole() == UserRole.TA).count();
long moCount = allUsers.stream()
        .filter(u -> u.getRole() == UserRole.MO).count();
long adminCount = allUsers.stream()
        .filter(u -> u.getRole() == UserRole.ADMIN).count();
req.setAttribute("taCount", taCount);
req.setAttribute("jobCount", allJobs.size());
req.setAttribute("applicationCount", allApplications.size());
```

---

### 2.2 Tianyu Zhao — TA Dashboard & Application & CV

**Responsible Files**:
- `UpdateProfileServlet.java`, `UploadCvServlet.java`, `DownloadCvServlet.java`
- `ApplyServlet.java`, `WithdrawApplicationServlet.java`
- `UserService.java`
- `dashboard-ta.jsp`
- `assets/css/app.css`, `assets/js/app.js`

**Backlog Stories**: #2 TA Profile, #5 TA Apply, #6 TA Upload CV

#### Feature Description (for Demo Presentation)

**TA Dashboard** (`/dashboard` — TA view)
- **Hero Section**: Welcome message + KPI cards showing current workload hours, active application count, and profile completeness percentage
- **Top Recommendation Spotlight**: Highlights the best-matching job with its score and fit label (e.g., "Strong Fit 82%")
- **Profile Management Form**: Inline editing of name, student ID, email, programme, skills (comma/semicolon/newline separated), availability, experience, and CV text
- **CV Upload Area**: Drag-and-drop or click to upload CV files (PDF, DOC, DOCX, TXT; max 5MB); old CV files are automatically deleted on re-upload
- **Application History Table**: Shows all submitted applications with job title, status badge (Submitted/Shortlisted/Accepted/Rejected/Withdrawn), recommendation score, and submission timestamp
- **Recommended Jobs Grid**: Searchable and sortable list of job cards, each displaying overall match percentage, matched/missing skill tags, 6-dimension progress bars, human-readable explanation reasons, and Apply/View Detail buttons

**Apply for Position** (`/apply`)
- One-click from dashboard or job detail page. Pre-submission validation ensures: profile is complete, job is OPEN and not expired, quota is not full, and no duplicate active application exists. Recommendation score is computed and stored at submission time.

**Withdraw Application** (`/applications/withdraw`)
- Available for SUBMITTED or SHORTLISTED applications. Sets status to WITHDRAWN. User can re-apply after withdrawal.

**Profile Update** (`/profile/update`)
- Input sanitisation: strips HTML tags, control characters, enforces max length. Email format is validated. Skills are parsed from comma/semicolon/newline-separated input.

**CV Upload** (`/profile/cv/upload`)
- Whitelist: pdf, doc, docx, txt. Size limit: 5MB. Stored as `{userId}_cv.{ext}`. Old CV auto-deleted.

**CV Download** (`/cv/download`)
- Role-based access control: Admin can download any CV; TA can download their own; MO can only download CVs of applicants who applied to their jobs.

**Demo Walkthrough**: Login as `alice.ta` → see the TA Dashboard with recommended jobs and KPI cards → scroll down to edit profile (add a skill like "Python") → upload a CV file → click "Apply" on a recommended job → see the application appear in the history table → click "Withdraw" to demonstrate withdrawal.

#### Implementation Details

**1. Profile Update with Input Sanitisation** (`UserService.java`)

```java
// UserService.java — updateTaProfile() (lines 51-97)
public ActionResult updateTaProfile(UserProfile actor,
        String name, String studentId, String email,
        String programme, String rawSkills, String availability,
        String experience, String cvText) {

    if (actor.getRole() != UserRole.TA) {
        return ActionResult.failure("Only TA accounts can update TA profiles.");
    }

    // Input cleaning: strip HTML tags, control chars, enforce max length
    name = cleanText(name, 100);
    studentId = cleanText(studentId, 30);
    email = cleanText(email, 150);

    // Email format validation
    if (email != null && !email.isEmpty()
            && !email.matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
        return ActionResult.failure("Please provide a valid email address.");
    }

    // Skill parsing: supports comma, semicolon, newline separators
    List<String> skills = (rawSkills == null || rawSkills.isBlank())
            ? List.of()
            : Arrays.stream(rawSkills.split("[,;\\n]+"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> cleanText(s, 80))
                    .toList();

    actor.setName(name);
    actor.setStudentId(studentId);
    // ... (set all fields)
    save(actor);
    return ActionResult.success("Profile updated successfully.");
}
```

**2. CV Upload with Security Controls** (`UploadCvServlet.java`)

```java
// UploadCvServlet.java — doPost() (lines 28-67)
Part cvPart = req.getPart("cvFile");
String submittedFileName = submittedFileName(cvPart);
String extension = fileExtension(submittedFileName);

// Whitelist check
if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
    setFlash(req, "error", "Only PDF, DOC, DOCX, TXT files are allowed.");
    redirect(req, resp, "/dashboard");
    return;
}

// Size check
if (cvPart.getSize() > MAX_FILE_SIZE) {
    setFlash(req, "error", "File size must not exceed 5 MB.");
    redirect(req, resp, "/dashboard");
    return;
}

// Delete old CV files before saving new one
String storedFileName = user.getUserId() + "_cv." + extension;
Path targetFile = AppPaths.cvDir().resolve(storedFileName).normalize();

try (Stream<Path> paths = Files.list(AppPaths.cvDir())) {
    paths.filter(Files::isRegularFile)
         .filter(path -> path.getFileName().toString()
                 .startsWith(userId + "_cv."))
         .forEach(path -> { try { Files.delete(path); }
                            catch (IOException ignored) {} });
}

try (InputStream inputStream = cvPart.getInputStream()) {
    Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
}
```

Security features:
- File extension whitelist: `{pdf, doc, docx, txt}`
- File size limit: 5MB
- Old CV cleanup before saving
- Path normalization to prevent traversal

**3. CV Download with Role-Based Access Control** (`DownloadCvServlet.java`)

```java
// DownloadCvServlet.java — isAllowedToAccess() (lines 50-71)
private boolean isAllowedToAccess(UserProfile viewer, UserProfile owner,
        HttpServletRequest req) {
    if (viewer.getRole() == UserRole.ADMIN) return true;
    if (viewer.getUserId().equals(owner.getUserId())) return true;
    if (viewer.getRole() == UserRole.MO) {
        String jobId = req.getParameter("jobId");
        if (jobId == null) return false;
        return services(req).jobService().findById(jobId)
                .filter(job -> job.getOwnerId().equals(viewer.getUserId()))
                .isPresent()
                && services(req).applicationService()
                        .findByJobId(jobId).stream()
                        .anyMatch(app -> app.getApplicantId()
                                .equals(owner.getUserId()));
    }
    return false;
}
```

---

### 2.3 Jie Ren — MO Dashboard & Job Management

**Responsible Files**:
- `CreateJobServlet.java`, `UpdateJobServlet.java`, `ChangeJobStatusServlet.java`
- `JobService.java`
- `dashboard-mo.jsp`
- `data/jobs/` (seed data)

**Backlog Stories**: #3 MO Post TA Position, #18 MO Edit/Close Job

#### Feature Description (for Demo Presentation)

**MO Dashboard** (`/dashboard` — MO view)
- **KPI Cards**: Open jobs count, total applications received, shortlisted count, accepted count
- **Job Overview Cards**: Each owned job displayed as a feature card showing module code, title, status (Open/Closed), deadline, quota, and workload hours per week
- **Create New Job Form**: Validated fields — title (required, max 200 chars), module code (required), deadline (must be future date), quota (positive integer), workload hours (positive integer), required skills (at least 1), preferred skills (optional), description (required). All text fields are cleaned of `<>` tags for XSS prevention.
- **Candidate Queues**: For each owned job, a table of applicants showing name, skills, current workload hours, recommendation score percentage with explanation summary, and a status dropdown (Submitted/Shortlisted/Accepted/Rejected) to review applications

**Create Job** (`/jobs/create`)
- MO fills in the form; system validates all fields, generates a unique job ID, saves to JSON, and writes an audit log entry.

**Edit Job** (`/jobs/update`)
- Only the job owner can edit. Quota cannot be reduced below the number of already-accepted applications. If deadline passes or quota fills after edit, the job automatically closes.

**Close / Reopen Job** (`/jobs/status`)
- Close sets the job to CLOSED. Reopen validates that the deadline hasn't passed and quota isn't full before setting back to OPEN. All status changes are logged.

**Automatic State Synchronisation**
- Every time the job list is queried, the system checks all OPEN jobs: if expired or fully filled, they are automatically set to CLOSED. This ensures data consistency without manual intervention.

**Demo Walkthrough**: Login as `mo.chen` → see the MO Dashboard with job overview cards and KPI stats → scroll to "Create New Job" form → fill in title "Python Lab Assistant", module "EBU6304", deadline, quota 3, skills "Python, Testing" → submit → see the new job card appear → click on a job to view candidates → use the status dropdown to accept/reject → click "Close Job" to demonstrate status toggle.

#### Implementation Details

**1. Job Creation with Comprehensive Validation** (`JobService.java`)

```java
// JobService.java — createJob() (lines 80-128)
public ActionResult createJob(UserProfile actor, String title,
        String moduleCode, String deadlineStr, String quotaStr,
        String workloadStr, String requiredSkillsRaw,
        String preferredSkillsRaw, String description) {

    if (actor.getRole() != UserRole.MO) {
        return ActionResult.failure("Only Module Organisers can create jobs.");
    }

    ActionResult validation = validateJobInput(title, moduleCode,
            deadlineStr, quotaStr, workloadStr, requiredSkillsRaw, description);
    if (!validation.success()) return validation;

    // Parse and clean all fields
    LocalDate deadline = LocalDate.parse(cleanText(deadlineStr, 10));
    int quota = Integer.parseInt(cleanText(quotaStr, 5));
    int workloadHours = Integer.parseInt(cleanText(workloadStr, 5));

    List<String> requiredSkills = parseSkills(requiredSkillsRaw);
    List<String> preferredSkills = parseSkills(preferredSkillsRaw);

    // Generate unique ID
    String jobId = idCounterRepository.nextId("job", "J");

    JobPosting job = new JobPosting();
    job.setJobId(jobId);
    job.setOwnerId(actor.getUserId());
    // ... (set all fields)
    job.setStatus(JobStatus.OPEN);
    job.setCreatedAt(Instant.now().toString());

    jobRepository.save(job);
    auditRepository.log("CREATE_JOB", actor.getUserId(), jobId);
    return ActionResult.success("Job '" + title + "' created successfully.");
}
```

**2. Input Validation Engine** (`JobService.java`)

```java
// JobService.java — validateJobInput() (lines 268-312)
private ActionResult validateJobInput(String title, String moduleCode,
        String deadlineStr, String quotaStr, String workloadStr,
        String requiredSkillsRaw, String description) {

    // Required field checks
    if (isBlank(title)) return ActionResult.failure("Title is required.");
    if (isBlank(moduleCode)) return ActionResult.failure("Module code is required.");
    if (isBlank(deadlineStr)) return ActionResult.failure("Deadline is required.");
    if (isBlank(description)) return ActionResult.failure("Description is required.");

    // Date validation
    try {
        LocalDate deadlineDate = LocalDate.parse(cleanText(deadlineStr, 10));
        if (deadlineDate.isBefore(LocalDate.now())) {
            return ActionResult.failure("Deadline must be in the future.");
        }
    } catch (Exception ex) {
        return ActionResult.failure("Please enter a valid deadline date.");
    }

    // Numeric validation
    try {
        int quota = Integer.parseInt(cleanText(quotaStr, 5));
        if (quota <= 0) return ActionResult.failure("Quota must be positive.");
    } catch (NumberFormatException ex) {
        return ActionResult.failure("Please enter a valid quota number.");
    }

    // Skills validation
    if (isBlank(requiredSkillsRaw)) {
        return ActionResult.failure("At least one required skill is needed.");
    }
    return ActionResult.success("Valid");
}
```

**3. Automatic State Synchronisation** (`JobService.java`)

```java
// JobService.java — synchronizeOperationalStates() (lines 245-260)
private List<JobPosting> synchronizeOperationalStates(List<JobPosting> jobs) {
    boolean changed = false;
    for (JobPosting job : jobs) {
        if (job.getStatus() == JobStatus.OPEN) {
            long acceptedCount = applicationRepository.findAll().stream()
                    .filter(a -> a.getJobId().equals(job.getJobId()))
                    .filter(a -> a.getStatus() == ApplicationStatus.ACCEPTED)
                    .count();

            boolean expired = job.getDeadline() != null
                    && LocalDate.parse(job.getDeadline()).isBefore(LocalDate.now());
            boolean quotaFull = acceptedCount >= job.getQuota();

            if (expired || quotaFull) {
                job.setStatus(JobStatus.CLOSED);
                jobRepository.save(job);
                changed = true;
            }
        }
    }
    return changed ? jobRepository.findAll() : jobs;
}
```

This ensures that expired or fully-filled jobs are automatically closed when the job list is queried.

---

### 2.4 Haopeng Jin — Recommendation Engine & Application Service & Search/Filter

**Responsible Files**:
- `RecommendationService.java` (626 lines — the most complex service)
- `ApplicationService.java` (415 lines)
- `JobDetailServlet.java`, `UpdateApplicationStatusServlet.java`
- `DashboardServlet.java` (search/filter logic for all 3 roles)
- `JobRecommendation.java` (view model)
- `job-detail.jsp`, `dashboard-ta.jsp` (search UI), `dashboard-mo.jsp` (search UI)
- `DownloadCvServlet.java` (friendly filename generation)
- `data/applications/` (seed data)

**Backlog Stories**: #4 Browse Positions, #8 Accept/Reject, #14 Job Search & Filter, #19 AI Skill Matching

#### Feature Description (for Demo Presentation)

**Cross-Role Search & Filter System** (added in v3.0.3)

TA Dashboard Search (`/dashboard` — TA view):
- **Keyword Search**: Text input matches against job title, module code, description, required skills, preferred skills, matched skills, and missing skills. Backend implementation in `DashboardServlet.matchesRecommendationQuery()` concatenates all searchable fields and uses `contains()`.
- **Skill Filter**: Comma-separated skill input (e.g., "Python, Java") matches against job required/preferred skills. Any single skill hit includes the job. Backend in `DashboardServlet.matchesSkillFilter()`.
- **Max Hours Filter**: Numeric input (e.g., 8) filters out jobs with weekly workload exceeding the value. Backend: `job.getWorkloadHours() <= maxHours`.
- **Deadline Filter**: Date input (yyyy-mm-dd format) shows only jobs with deadline on or before the specified date. Backend: `LocalDate.parse(job.getDeadline()).isBefore(deadlineBefore)`.
- **Sort Options**: Best match first (default) / Closest deadline / Lowest projected workload.
- **Clear Filters**: "Clear filters" button resets all filters and returns to the full recommended list.

MO Dashboard Search (`/dashboard` — MO view):
- **Search Bar**: Keyword input matches job title, module code, skills, and description. Backend in `DashboardServlet.matchesMoSearch()`.
- **Enhanced Candidate Table**: Added CV download column and programme information display.

Admin Dashboard Search (`/dashboard` — Admin view):
- **Extended Search Scope**: Now also matches skill keywords in addition to module code and title. Backend in `DashboardServlet.matchesAdminFilter()`.

**CV Download Filename Optimisation**:
- Downloaded CV files are automatically named `{Name}_{ModuleCode}.{ext}` (e.g., `Alice_Zhang_EBU6304.pdf`) instead of the raw `U1001_cv.pdf`, making it easier for MOs to identify and file CVs.

**Demo Walkthrough**: Login as `alice.ta` → type "Python" in the search box → only jobs requiring Python appear → type "Java, Testing" in the skill filter → results narrow further → set max hours to 6 → only lightweight roles remain → click "Clear filters" to reset → switch sort to "Closest deadline".

**Recommendation Engine** — the core innovation of RecruitAssist
- Evaluates each TA-Job pair across **6 weighted dimensions** with configurable weights from `config.json`:
  - **Skill Match (40%)**: Required skill coverage ×0.72 + preferred ×0.18 + breadth bonus + full coverage bonus − gap penalty. Uses three-level matching: (1) canonical exact match → (2) Jaccard similarity ≥0.55 fuzzy match → (3) token containment. Includes skill alias normalization (e.g., "OOP" ↔ "Object Oriented Programming", 8 alias groups).
  - **Experience (18%)**: Base 0.3 + bonuses from phrase coverage, token overlap, and evidence keyword hits (lab, marking, debugging, etc., 16 keywords).
  - **Availability (12%)**: Base 0.34 + keyword bonuses for weekday/weekend, specific days, time slots, flexibility.
  - **Workload Balance (12%)**: Under threshold → 0.55 + remaining ratio ×0.45. Over → penalty proportional to overshoot. Promotes fair distribution.
  - **Profile Evidence (10%)**: 5-field completeness (58%) + job alignment (22%) + breadth signal (20%).
  - **Competition (8%)**: Active applicants per remaining slot. ≤1/slot → 0.95; progressively lower for higher competition.
- **Explainability**: Each dimension generates a natural-language explanation displayed on the TA dashboard and job detail page.

**Job Detail Page** (`/jobs/detail`)
- **TA view**: Shows the recommendation match snapshot with 6-dimension progress bars, matched/missing skills, explanation reasons, and Apply/Withdraw buttons
- **MO view**: Shows an editable job form + candidate table with multi-dimensional sorting (by score/workload/submitted/status) and status filtering + status update dropdowns
- **Admin view**: Read-only overview with candidate statistics
- Displays real-time applicant count, accepted/quota ratio, and remaining slots

**Application Status Management** (`/applications/status`)
- MO can transition: SUBMITTED → SHORTLISTED/ACCEPTED/REJECTED, SHORTLISTED → ACCEPTED/REJECTED
- When the last quota slot is filled by ACCEPTED, the job automatically closes
- All transitions are logged in the audit trail

**Multi-Dimensional Candidate Sorting**
- Sort by: score (default, 4-level comparator), workload (ascending), submitted time (newest first), status (priority order: Accepted > Shortlisted > Submitted > Rejected > Withdrawn)

**Demo Walkthrough**: Login as `alice.ta` → on dashboard, see recommended jobs with match percentages → click "View Detail" on a job → see the 6-dimension progress bars and explanation reasons → apply for the job → logout → login as `mo.chen` → go to that job's detail page → see candidates sorted by recommendation score → use dropdown to accept the top candidate → notice the quota counter updates → if quota fills, the job auto-closes.

#### Implementation Details

**1. Six-Dimensional Recommendation Scoring** (`RecommendationService.java`)

The core recommendation algorithm evaluates each TA-Job pair:

```java
// RecommendationService.java — recommend() (lines 103-180)
public JobRecommendation recommend(UserProfile user, JobPosting job) {
    SkillProfile skillProfile = buildSkillProfile(user);
    Set<String> profileTokens = tokenize(buildProfileText(user));
    Set<String> jobKeywords = tokenize(buildJobText(job));

    // Matched / missing skills
    List<String> matchedRequired = new ArrayList<>();
    List<String> matchedPreferred = new ArrayList<>();
    List<String> missingSkills = new ArrayList<>();

    for (String skill : job.getRequiredSkills()) {
        if (matchesSkill(skillProfile, skill, profileTokens)) {
            matchedRequired.add(skill);
        } else {
            missingSkills.add(skill);
        }
    }

    // Six dimension scores
    double skillScore = calculateSkillScore(skillProfile,
            matchedRequired, matchedPreferred, missingSkills,
            job.getRequiredSkills().size(), job.getPreferredSkills().size());

    double availScore = calculateAvailabilityScore(user.getAvailability());

    double expScore = calculateExperienceScore(user, job,
            profileTokens, jobKeywords, matchedRequired);

    double profileEvidence = calculateProfileEvidenceScore(user, job,
            matchedRequired, matchedPreferred, profileTokens);

    int currentWorkload = workloadService.workloadForUser(user.getUserId());
    double workloadBalance = calculateWorkloadBalance(
            currentWorkload, job.getWorkloadHours(),
            workloadService.getThreshold());

    CompetitionSnapshot competition = calculateCompetition(job);

    // Weighted average using configurable weights
    RecommendationConfig cfg = configRepo.load().getRecommendation();
    double totalWeight = cfg.getSkillMatchWeight() + cfg.getAvailabilityWeight()
            + cfg.getExperienceWeight() + cfg.getWorkloadBalanceWeight()
            + cfg.getProfileEvidenceWeight() + cfg.getCompetitionWeight();

    double score = (skillScore * cfg.getSkillMatchWeight()
            + availScore * cfg.getAvailabilityWeight()
            + expScore * cfg.getExperienceWeight()
            + workloadBalance * cfg.getWorkloadBalanceWeight()
            + profileEvidence * cfg.getProfileEvidenceWeight()
            + competition.score() * cfg.getCompetitionWeight())
            / totalWeight;

    // Generate explanation reasons
    List<String> reasons = buildReasons(...);

    return new JobRecommendation(job, score, skillScore, availScore,
            expScore, workloadBalance, profileEvidence,
            competition.score(), matchedRequired, matchedPreferred,
            missingSkills, reasons, collectEvidenceHits(...));
}
```

**2. Skill Matching with Alias Normalization & Fuzzy Matching**

```java
// RecommendationService.java — matchesSkill() (lines 196-209)
private boolean matchesSkill(SkillProfile profile, String jobSkill,
        Set<String> profileTokens) {
    String canonJob = canonicalizeSkill(jobSkill);

    // Level 1: Canonical exact match
    if (profile.canonicalSkills().contains(canonJob)) return true;

    // Level 2: Jaccard similarity >= 0.55
    Set<String> jobTokens = tokenize(canonJob);
    for (String declaredSkill : profile.declaredSkills()) {
        if (jaccard(jobTokens, tokenize(canonicalizeSkill(declaredSkill)))
                >= 0.55) {
            return true;
        }
    }

    // Level 3: Token containment
    return profileTokens.containsAll(jobTokens);
}

// Jaccard similarity (lines 542-549)
private double jaccard(Set<String> left, Set<String> right) {
    if (left.isEmpty() && right.isEmpty()) return 1.0;
    Set<String> intersection = new HashSet<>(left);
    intersection.retainAll(right);
    Set<String> union = new HashSet<>(left);
    union.addAll(right);
    return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
}
```

**3. Application State Machine** (`ApplicationService.java`)

```java
// ApplicationService.java — submitApplication() (lines 164-212)
public ActionResult submitApplication(UserProfile user, String jobId) {
    lock.writeLock().lock();
    try {
        // Validation chain
        if (user.getRole() != UserRole.TA)
            return ActionResult.failure("Only TAs can apply.");
        if (!isProfileSufficient(user))
            return ActionResult.failure("Please complete your profile first.");

        Optional<JobPosting> jobOpt = jobService.findById(jobId);
        if (jobOpt.isEmpty())
            return ActionResult.failure("Job not found.");

        JobPosting job = jobOpt.get();
        if (job.getStatus() != JobStatus.OPEN)
            return ActionResult.failure("This position is no longer open.");

        // Duplicate check (exclude withdrawn)
        if (findExistingApplication(user.getUserId(), jobId).isPresent())
            return ActionResult.failure("You already have an active application.");

        // Quota check
        long acceptedCount = countAccepted(jobId);
        if (acceptedCount >= job.getQuota())
            return ActionResult.failure("This position is fully filled.");

        // Compute recommendation score
        JobRecommendation rec = recommendationService.recommend(user, job);

        // Create application record
        String appId = idCounterRepository.nextId("application", "A");
        ApplicationRecord application = new ApplicationRecord();
        application.setApplicationId(appId);
        application.setApplicantId(user.getUserId());
        application.setJobId(jobId);
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setRecommendationScore(rec.getScore());
        application.setRecommendationExplanation(rec.getReasons());
        application.setSubmittedAt(Instant.now().toString());

        applicationRepository.save(application);
        auditRepository.log("SUBMIT_APPLICATION", user.getUserId(), appId);
        return ActionResult.success("Application submitted! Score: "
                + rec.getScorePercent() + "%");
    } finally {
        lock.writeLock().unlock();
    }
}
```

**4. Multi-Dimensional Candidate Sorting** (`ApplicationService.java`)

```java
// ApplicationService.java — sortForReview() (lines 291-324)
private List<ApplicationRecord> sortForReview(List<ApplicationRecord> list,
        String sortBy, Map<String, Integer> workload) {
    Comparator<ApplicationRecord> comparator = switch (normalizeReviewSort(sortBy)) {
        case "score" -> Comparator
                .comparingDouble(ApplicationRecord::getRecommendationScore).reversed()
                .thenComparing(a -> statusPriority(a.getStatus()))
                .thenComparing(a -> workload.getOrDefault(a.getApplicantId(), 0))
                .thenComparing(ApplicationRecord::getSubmittedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()));
        case "workload" -> Comparator
                .comparingInt((ApplicationRecord a) ->
                        workload.getOrDefault(a.getApplicantId(), 0))
                .thenComparingDouble(ApplicationRecord::getRecommendationScore)
                        .reversed()
                .thenComparing(ApplicationRecord::getSubmittedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()));
        case "submitted" -> Comparator
                .comparing(ApplicationRecord::getSubmittedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparingDouble(ApplicationRecord::getRecommendationScore)
                        .reversed();
        case "status" -> Comparator
                .comparingInt((ApplicationRecord a) -> statusPriority(a.getStatus()))
                .thenComparingDouble(ApplicationRecord::getRecommendationScore)
                        .reversed();
        default -> Comparator.comparingDouble(
                ApplicationRecord::getRecommendationScore).reversed();
    };
    return list.stream().sorted(comparator).toList();
}
```

---

### 2.5 Zhuang Hou — Admin Dashboard & Workload Monitoring

**Responsible Files**:
- `DashboardServlet.java` (224 lines — routing + 3 render methods)
- `WorkloadService.java`
- `WorkloadEntry.java`
- `SystemConfig.java`
- `dashboard-admin.jsp`
- `data/system/config.json`

**Backlog Stories**: #10 Admin Overview Dashboard, #21 Workload Monitoring

#### Feature Description (for Demo Presentation)

**Dashboard Routing** (`/dashboard`)
- The `DashboardServlet` is the central routing hub: after authentication, it dispatches to `renderTaDashboard()`, `renderMoDashboard()`, or `renderAdminDashboard()` based on the user's role. This means all three roles share the same `/dashboard` URL but see entirely different pages.

**Admin Dashboard** (`/dashboard` — Admin view)
- **KPI Cards**: Number of tracked TAs, recent applications count, policy workload threshold (default 12h/week), number of filtered jobs
- **Recruitment Overview Table**: Lists ALL jobs in the system, filterable by status (Open/Closed) and searchable by module code or title. Columns: Job title, Owner (MO name), Deadline, Applicant count, Accepted/Quota ratio, Status badge
- **TA Workload Table**: Lists ALL TAs with their accepted hours, active application count, and a "Balanced" / "Over threshold" indicator. Sorted by workload hours descending to highlight potential overload. The threshold is configurable in `config.json` (default: 12 hours/week).
- **Recent Applications**: The latest 10 applications system-wide, showing applicant name, job title, status, recommendation score, and submission time

**Workload Monitoring**
- The `WorkloadService` calculates each TA's total workload by summing the `workloadHours` of all jobs where their application status is ACCEPTED
- `buildEntries()` creates a sorted list of `WorkloadEntry` objects for the Admin dashboard, each containing: user profile, accepted hours, active application count, and an overloaded flag

**TA Dashboard Search & Filter** (also implemented in `DashboardServlet`)
- TA view supports keyword search across job title, module code, description, skills, and matched skills
- Sort options: recommendation score (default), deadline, workload impact

**Demo Walkthrough**: Login as `admin.sarah` → see the Admin Dashboard with KPI cards → browse the Recruitment Overview table → filter by "Open" status → search for a specific module code → scroll to the TA Workload table to check if any TA is "Over threshold" → review the recent applications list.

#### Implementation Details

**1. Role-Based Dashboard Routing** (`DashboardServlet.java`)

```java
// DashboardServlet.java — doGet() (lines 25-45)
@Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
    UserProfile user = requireAuthenticatedUser(req, resp);
    if (user == null) return;

    moveFlashToRequest(req);
    req.setAttribute("user", user);
    req.setAttribute("currentPage", "dashboard");

    switch (user.getRole()) {
        case TA    -> renderTaDashboard(req, resp, user);
        case MO    -> renderMoDashboard(req, resp, user);
        case ADMIN -> renderAdminDashboard(req, resp, user);
    }
}
```

**2. Admin Dashboard with Filtering** (`DashboardServlet.java`)

```java
// DashboardServlet.java — renderAdminDashboard() (lines 109-145)
private void renderAdminDashboard(HttpServletRequest req,
        HttpServletResponse resp, UserProfile user)
        throws ServletException, IOException {

    AppServices svc = services(req);
    List<JobPosting> allJobs = svc.jobService().listAllJobs();

    // Status filter
    String statusFilter = req.getParameter("jobStatusFilter");
    if (statusFilter != null && !statusFilter.isBlank()) {
        JobStatus filterStatus = JobStatus.valueOf(statusFilter.toUpperCase());
        allJobs = allJobs.stream()
                .filter(j -> j.getStatus() == filterStatus).toList();
    }

    // Module search
    String moduleQuery = req.getParameter("moduleQuery");
    if (moduleQuery != null && !moduleQuery.isBlank()) {
        String q = moduleQuery.toLowerCase();
        allJobs = allJobs.stream()
                .filter(j -> j.getModuleCode().toLowerCase().contains(q)
                        || j.getTitle().toLowerCase().contains(q))
                .toList();
    }

    // Workload entries
    List<WorkloadEntry> entries = svc.workloadService().buildEntries();
    List<ApplicationRecord> latest = svc.applicationService()
            .findRecentApplications(10);

    req.setAttribute("adminJobs", allJobs);
    req.setAttribute("workloadEntries", entries);
    req.setAttribute("latestApplications", latest);
    // ... forward to dashboard-admin.jsp
}
```

**3. Workload Calculation Algorithm** (`WorkloadService.java`)

```java
// WorkloadService.java — workloadByUserId() (lines 48-66)
public Map<String, Integer> workloadByUserId() {
    Map<String, Integer> result = new HashMap<>();
    Map<String, JobPosting> jobIndex = jobService.indexById();

    for (ApplicationRecord app : applicationRepository.findAll()) {
        if (app.getStatus() != ApplicationStatus.ACCEPTED) continue;

        JobPosting job = jobIndex.get(app.getJobId());
        if (job == null) continue;

        result.merge(app.getApplicantId(), job.getWorkloadHours(), Integer::sum);
    }
    return result;
}

// WorkloadService.java — buildEntries() (lines 68-79)
public List<WorkloadEntry> buildEntries() {
    Map<String, Integer> workload = workloadByUserId();
    int threshold = getThreshold();
    return userService.listUsersByRole(UserRole.TA).stream()
            .map(ta -> {
                int hours = workload.getOrDefault(ta.getUserId(), 0);
                long active = activeApplicationsForUser(ta.getUserId());
                return new WorkloadEntry(ta, hours, active, hours > threshold);
            })
            .sorted(Comparator.comparingInt(WorkloadEntry::getAcceptedHours)
                    .reversed())
            .toList();
}
```

---

### 2.6 Zexuan Dong — Data Layer & Infrastructure & Testing

**Responsible Files**:
- `JsonFileStore.java` (174 lines — core persistence engine)
- `AppPaths.java`, `AppServices.java`, `AppBootstrapListener.java`, `AppContextKeys.java`
- `AppServlet.java` (abstract base class)
- `AuthService.java`
- All Repository classes: `UserRepository.java`, `JobRepository.java`, `ApplicationRepository.java`, `SystemConfigRepository.java`, `IdCounterRepository.java`, `AuditRepository.java`
- All Model classes: `UserProfile.java`, `JobPosting.java`, `ApplicationRecord.java`, `ActionResult.java`, `SystemConfig.java`, `UserRole.java`, `JobStatus.java`, `ApplicationStatus.java`
- `AppPathsTest.java`
- `scripts/generate_demo_load.py`, `scripts/mvn17.sh`

**Backlog Stories**: #11 JSON Data Persistence, #12 Registration & Auth, #24 Testing

#### Feature Description (for Demo Presentation)

**JSON Data Persistence Layer** (`JsonFileStore.java`)
- All data is stored as JSON/CSV/TXT files — no database required. The `JsonFileStore` is the core persistence engine providing:
  - **Two-level caching**: File-level cache (keyed by path + modification time + file size) and directory-level cache. Cache hits avoid disk I/O entirely.
  - **Path-level read-write locks**: Each file path gets its own `ReentrantReadWriteLock` via `ConcurrentHashMap.computeIfAbsent()`, enabling fine-grained concurrent access — multiple readers can read different files simultaneously.
  - **Atomic writes**: Data is first written to a `.tmp` temp file, then atomically moved to the target path using `Files.move(ATOMIC_MOVE)`. This prevents partial writes from corrupting data.
  - **Automatic cache invalidation**: Write operations remove the file from cache and invalidate the parent directory cache by prefix matching.

**Smart Path Resolution** (`AppPaths.java`)
- The system auto-detects the project root directory using a 3-level priority: (1) Java system property `recruitassist.baseDir` → (2) environment variable `RECRUITASSIST_BASE_DIR` → (3) auto-detect from CWD (if running from `framework/recruitassist-web`, automatically goes up two levels). This means the app works correctly regardless of where you run `mvn` from.

**Dependency Injection Container** (`AppServices.java`)
- On startup, `AppBootstrapListener` initializes the `AppServices` singleton which creates: `JsonFileStore` → 6 Repositories → 6 Services in dependency order. All services receive their dependencies via constructor injection.

**Authentication** (`AuthService.java`)
- Accepts username and password, trims whitespace, looks up user by username, and compares passwords. Returns `Optional<UserProfile>` — empty on failure, present on success. (Note: currently plaintext comparison for demo purposes.)

**Data Structure**: `data/users/` (UserProfile JSON), `data/jobs/` (JobPosting JSON), `data/applications/` (ApplicationRecord JSON), `data/cv/` (uploaded files), `data/system/config.json` (system config), `data/system/id-counters.json` (auto-increment IDs), `logs/access/audit.csv` (audit trail).

**Audit Logging** (`AuditRepository.java`)
- Every write operation (create job, submit application, update status, etc.) appends a line to `audit.csv` with timestamp, action type, user ID, and target ID.

**Demo Walkthrough**: The data layer is invisible to end users but powers everything. To demonstrate: show the `data/` directory structure → open a user JSON file to show the schema → perform an action (e.g., apply for a job) → show the new application JSON file created in `data/applications/` → show the audit log entry in `logs/access/audit.csv`.

#### Implementation Details

**1. Cached JSON File Store with Concurrency Control** (`JsonFileStore.java`)

```java
// JsonFileStore.java — read() with two-level caching (lines 65-93)
public <T> T read(Path file, Type type) {
    ReentrantReadWriteLock rwLock = pathLock(file);
    rwLock.readLock().lock();
    try {
        // Check file-level cache
        FileCacheEntry cached = fileCache.get(file);
        long modifiedAt = Files.getLastModifiedTime(file).toMillis();
        long size = Files.size(file);

        if (cached != null && cached.type().equals(type)
                && cached.modifiedAt() == modifiedAt
                && cached.size() == size) {
            return (T) cached.value(); // Cache hit
        }

        // Cache miss: deserialize from file
        String content = Files.readString(file, StandardCharsets.UTF_8);
        T value = gson.fromJson(content, type);

        // Update cache
        fileCache.put(file, new FileCacheEntry(type, modifiedAt, size, value));
        return value;
    } finally {
        rwLock.readLock().unlock();
    }
}
```

```java
// JsonFileStore.java — atomic write (lines 95-126)
public void write(Path file, Object value) {
    ReentrantReadWriteLock rwLock = pathLock(file);
    rwLock.writeLock().lock();
    try {
        Files.createDirectories(file.getParent());

        // Write to temp file first
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        String json = gson.toJson(value);
        Files.writeString(temp, json, StandardCharsets.UTF_8);

        // Atomic move: prevents partial writes
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);

        // Invalidate caches
        fileCache.remove(file);
        invalidateDirectoryCache(file.getParent());
    } finally {
        rwLock.writeLock().unlock();
    }
}
```

**2. Smart Path Resolution** (`AppPaths.java`)

```java
// AppPaths.java — resolveBaseDir() (lines 16-42)
private static Path resolveBaseDir() {
    // Priority 1: Java system property
    String prop = System.getProperty("recruitassist.baseDir");
    if (prop != null && !prop.isBlank()) return Path.of(prop);

    // Priority 2: Environment variable
    String env = System.getenv("RECRUITASSIST_BASE_DIR");
    if (env != null && !env.isBlank()) return Path.of(env);

    // Priority 3: Auto-detect from CWD
    Path cwd = Path.of("").toAbsolutePath();
    // If running from framework/recruitassist-web, go up two levels
    if (cwd.endsWith(Path.of("framework", "recruitassist-web"))
            || cwd.endsWith(Path.of("framework/recruitassist-web"))) {
        return cwd.getParent().getParent();
    }
    return cwd;
}
```

**3. Dependency Injection Container** (`AppServices.java`)

```java
// AppServices.java — init() (lines 22-68)
public static AppServices init() {
    JsonFileStore store = new JsonFileStore();

    // Repository layer
    UserRepository userRepo = new UserRepository(store);
    JobRepository jobRepo = new JobRepository(store);
    ApplicationRepository appRepo = new ApplicationRepository(store);
    SystemConfigRepository configRepo = new SystemConfigRepository(store);
    IdCounterRepository idRepo = new IdCounterRepository(store);
    AuditRepository auditRepo = new AuditRepository(store);

    // Service layer (with dependency injection)
    UserService userService = new UserService(userRepo);
    AuthService authService = new AuthService(userService);
    JobService jobService = new JobService(jobRepo, appRepo, idRepo,
            auditRepo, configRepo);
    WorkloadService workloadService = new WorkloadService(appRepo,
            jobService, userService, configRepo);
    RecommendationService recService = new RecommendationService(
            jobService, appRepo, workloadService, configRepo);
    ApplicationService appService = new ApplicationService(appRepo,
            jobService, userService, recService, workloadService,
            idRepo, auditRepo);

    return new AppServices(authService, userService, jobService,
            appService, workloadService, recService);
}
```

**4. ID Counter with File-Based Auto-Increment** (`IdCounterRepository.java`)

```java
// IdCounterRepository.java — nextId() (lines 25-45)
public synchronized String nextId(String entity, String prefix) {
    Map<String, Integer> counters = loadCounters();
    int next = counters.getOrDefault(entity, 0) + 1;
    counters.put(entity, next);
    saveCounters(counters);
    return prefix + next;
}
```

---

## 3. Cross-Cutting Concerns

### Flash Message System (Yi Qi + all Servlets)

Implemented in `AppServlet.java`, used by all Servlets for one-time user notifications:

```java
// AppServlet.java — setFlash/moveFlashToRequest (lines 58-80)
protected void setFlash(HttpServletRequest req, String tone, String message) {
    req.getSession().setAttribute("flashTone", tone);
    req.getSession().setAttribute("flashMessage", message);
}

protected void moveFlashToRequest(HttpServletRequest req) {
    HttpSession session = req.getSession(false);
    if (session != null) {
        Object tone = session.getAttribute("flashTone");
        Object message = session.getAttribute("flashMessage");
        if (tone != null) { req.setAttribute("flashTone", tone); session.removeAttribute("flashTone"); }
        if (message != null) { req.setAttribute("flashMessage", message); session.removeAttribute("flashMessage"); }
    }
}
```

### Audit Logging (Zexuan Dong — infrastructure, used by all write operations)

```java
// AuditRepository.java — log() (lines 18-25)
public void log(String action, String userId, String targetId) {
    String line = String.join(",",
            Instant.now().toString(), action, userId,
            targetId != null ? targetId : "");
    store.appendLine(AppPaths.auditLogFile(), line);
}
```

---

## 4. Product Backlog Coverage

| # | Story | Status | Primary Implementer |
|---|-------|--------|-------------------|
| 1 | User Login & Role-Based Access | ✅ Done | Yi Qi |
| 2 | TA Profile Creation & Editing | ✅ Done | Tianyu Zhao |
| 3 | MO Post TA Position | ✅ Done | Jie Ren |
| 4 | Browse Available TA Positions | ✅ Done | Haopeng Jin |
| 5 | TA Apply for Position | ✅ Done | Tianyu Zhao |
| 6 | TA Upload CV | ✅ Done | Tianyu Zhao |
| 7 | MO View Applicant List & CV | ✅ Done | Jie Ren + Haopeng Jin |
| 8 | MO Accept / Reject Application | ✅ Done | Haopeng Jin |
| 9 | TA Check Application Status | ✅ Done | Tianyu Zhao (dashboard-ta.jsp) |
| 10 | Admin Overview Dashboard | ✅ Done | Zhuang Hou |
| 11 | JSON Data Persistence Layer | ✅ Done | Zexuan Dong |
| 12 | User Registration & Auth | ✅ Done | Zexuan Dong (AuthService) |
| 13 | Responsive & Consistent UI | ✅ Done | Yi Qi + Tianyu Zhao |
| 14 | TA Job Search & Filter | ✅ Done | Zhuang Hou (DashboardServlet search) |
| 15 | Admin Historical Records | ✅ Done | Zhuang Hou (admin dashboard) |
| 16 | Real-Time Applicant Count | ✅ Done | Haopeng Jin (JobDetailServlet) |
| 17 | Privacy & Role-Based Access | ✅ Done | Zexuan Dong (AppServlet) + Tianyu Zhao (DownloadCvServlet) |
| 18 | MO Edit / Close Job | ✅ Done | Jie Ren |
| 19 | AI Skill Matching & Ranking | ✅ Done | Haopeng Jin |
| 20 | AI Missing Skills Identification | ✅ Done | Haopeng Jin |
| 21 | Workload Monitoring | ✅ Done | Zhuang Hou |
