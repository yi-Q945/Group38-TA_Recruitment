<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>MO Dashboard · RecruitAssist</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css" />
    <script defer src="${pageContext.request.contextPath}/assets/js/app.js"></script>
</head>
<body>
<div class="page-shell">
    <header class="hero-card home-hero dashboard-hero">
        <div>
            <div class="badge">MO Dashboard</div>
            <h1>${user.name}</h1>
            <p class="subtitle">Create and publish TA positions for your module. Sprint 1 scope focuses on posting jobs and showing the positions you already own.</p>
            <div class="hero-metrics">
                <div class="hero-metric"><strong>${jobs.size()}</strong><span>Total owned jobs</span></div>
            </div>
        </div>
        <aside class="spotlight-card">
            <div class="spotlight-kicker">Sprint 1 scope</div>
            <div class="spotlight-score">Post<span> jobs</span></div>
            <h3>Minimal MO workflow</h3>
            <p class="muted-copy">This version keeps only job publishing and owned-job display. Applicant review, status switching, and job editing belong to later sprints.</p>
            <div class="action-row">
                <a class="secondary-button small-button" href="${pageContext.request.contextPath}/logout">Log out</a>
            </div>
        </aside>
    </header>

    <c:if test="${not empty flashMessage}">
        <div class="alert ${flashTone}">${flashMessage}</div>
    </c:if>

    <section class="panel">
        <div class="section-head">
            <div>
                <h2>Create a new job</h2>
                <p class="muted-copy">Fill in the required fields below. New jobs are stored as JSON and published with an Open status.</p>
            </div>
            <span class="metric-pill">Sprint 1 story: MO Post TA Position</span>
        </div>
        <form class="form-grid two-column-form" method="post" action="${pageContext.request.contextPath}/jobs/create">
            <label class="field-group">
                <span>Job title</span>
                <input class="input" type="text" name="title" placeholder="e.g. Software Engineering TA" required />
            </label>
            <label class="field-group">
                <span>Module code</span>
                <input class="input" type="text" name="moduleCode" placeholder="e.g. EBU6304" required />
            </label>
            <label class="field-group">
                <span>Deadline</span>
                <input class="input" type="date" name="deadline" required />
            </label>
            <label class="field-group">
                <span>TA count / quota</span>
                <input class="input" type="number" name="quota" min="1" step="1" placeholder="1" required />
            </label>
            <label class="field-group">
                <span>Weekly hours</span>
                <input class="input" type="number" name="workloadHours" min="1" step="1" placeholder="4" required />
            </label>
            <div class="field-group full-width">
                <span>Required skills</span>
                <textarea class="textarea" name="requiredSkills" rows="3" placeholder="Java, OOP, Communication" required></textarea>
            </div>
            <div class="field-group full-width">
                <span>Preferred skills</span>
                <textarea class="textarea" name="preferredSkills" rows="3" placeholder="Testing, Marking, Leadership"></textarea>
            </div>
            <div class="field-group full-width">
                <span>Description</span>
                <textarea class="textarea" name="description" rows="4" placeholder="Describe what the TA will support in this role." required></textarea>
            </div>
            <div class="form-actions full-width">
                <button class="primary-button" type="submit" data-loading-text="Publishing job...">Publish job</button>
            </div>
        </form>
    </section>

    <c:if test="${empty jobs}">
        <section class="panel section-gap">
            <div class="empty-state large-empty">No jobs have been assigned to this module organiser yet.</div>
        </section>
    </c:if>

    <c:if test="${not empty jobs}">
        <section class="panel section-gap">
            <div class="section-head">
                <div>
                    <h2>Your published jobs</h2>
                    <p class="muted-copy">These jobs are already stored and visible in the system. This section is kept read-only for Sprint 1.</p>
                </div>
                <span class="metric-pill">${jobs.size()} roles</span>
            </div>
            <div class="showcase-grid">
                <c:forEach var="job" items="${jobs}">
                    <article class="feature-card">
                        <div class="section-head">
                            <div>
                                <div class="spotlight-kicker">${job.moduleCode}</div>
                                <h3>${job.title}</h3>
                            </div>
                            <span class="status-pill status-${job.status.cssClass}">${job.status.label}</span>
                        </div>
                        <p class="muted-copy">${job.description}</p>
                        <div class="job-meta-row section-gap">
                            <span class="meta-chip">Job ID: ${job.jobId}</span>
                            <span class="meta-chip">Quota: ${job.quota}</span>
                            <span class="meta-chip">Weekly hours: ${job.workloadHours}</span>
                            <span class="meta-chip">Deadline: ${job.deadlineLabel}</span>
                        </div>
                        <div class="job-meta-row section-gap">
                            <span class="meta-chip">Required: ${job.requiredSkillsSummary}</span>
                            <span class="meta-chip">Preferred: ${job.preferredSkillsSummary}</span>
                        </div>
                    </article>
                </c:forEach>
            </div>
        </section>
    </c:if>
</div>
</body>
</html>
