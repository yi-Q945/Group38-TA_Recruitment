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
<body data-auto-refresh-seconds="${autoRefreshSeconds}">
<div class="page-shell">
    <header class="hero-card home-hero dashboard-hero">
        <div>
            <div class="badge">MO Dashboard</div>
            <h1>${user.name}</h1>
            <p class="subtitle">Review applicants, manage the full job lifecycle and keep candidate decisions grounded in clearer queue summaries.</p>
            <div class="hero-metrics">
                <div class="hero-metric"><strong>${jobs.size()}</strong><span>Total owned jobs</span></div>
                <div class="hero-metric"><strong>${openJobCount}</strong><span>Currently open</span></div>
                <div class="hero-metric"><strong>${totalApplicationCount}</strong><span>Total candidate records</span></div>
            </div>
        </div>
        <aside class="spotlight-card">
            <div class="spotlight-kicker">Recruitment overview</div>
            <div class="spotlight-score">${acceptedCandidateCount}<span> accepted</span></div>
            <h3>Shortlist pressure at a glance</h3>
            <p class="muted-copy">You currently have ${shortlistedCandidateCount} shortlisted candidates across your modules. This dashboard auto-refreshes every ${autoRefreshSeconds} seconds when idle.</p>
            <div class="spotlight-meta">
                <span class="metric-pill">Default review order</span>
                <span class="metric-pill">Live queue refresh</span>
                <span class="metric-pill refresh-pill" data-refresh-countdown>Refresh in ${autoRefreshSeconds}s</span>
            </div>
            <div class="action-row">
                <a class="secondary-button small-button" href="${pageContext.request.contextPath}/logout">Log out</a>
            </div>
        </aside>
    </header>

    <c:if test="${not empty flashMessage}">
        <div class="alert ${flashTone}">${flashMessage}</div>
    </c:if>

    <section class="kpi-grid">
        <article class="kpi-card">
            <span class="kpi-label">Open jobs</span>
            <strong>${openJobCount}</strong>
            <p>Roles still accepting applications right now.</p>
        </article>
        <article class="kpi-card">
            <span class="kpi-label">Applications</span>
            <strong>${totalApplicationCount}</strong>
            <p>Total candidate records across all jobs you own.</p>
        </article>
        <article class="kpi-card">
            <span class="kpi-label">Shortlisted</span>
            <strong>${shortlistedCandidateCount}</strong>
            <p>Candidates currently in the active review shortlist.</p>
        </article>
        <article class="kpi-card">
            <span class="kpi-label">Accepted</span>
            <strong>${acceptedCandidateCount}</strong>
            <p>Applicants who already count toward teaching workload allocation.</p>
        </article>
    </section>

    <c:if test="${not empty jobs}">
        <section class="panel">
            <div class="section-head">
                <div>
                    <h2>Your active recruiting jobs</h2>
                    <p class="muted-copy">These are the specific roles currently owned by your recruiter account. Jump straight into each queue or open the full detail view.</p>
                </div>
                <span class="metric-pill">${jobs.size()} owned roles</span>
            </div>
            <div class="showcase-grid">
                <c:forEach var="job" items="${jobs}">
                    <c:set var="jobApplications" value="${applicationsByJobId[job.jobId]}" />
                    <article class="feature-card">
                        <div class="section-head">
                            <div>
                                <div class="spotlight-kicker">${job.moduleCode}</div>
                                <h3>${job.title}</h3>
                            </div>
                            <span class="status-pill status-${job.status.cssClass}">${job.status.label}</span>
                        </div>
                        <p class="muted-copy">${job.deadlineStatusLabel} · quota ${job.quota} · workload ${job.workloadHours}h</p>
                        <div class="job-meta-row section-gap">
                            <span class="meta-chip">Job ID: ${job.jobId}</span>
                            <span class="meta-chip">Applications: ${jobApplications.size()}</span>
                            <span class="meta-chip">Deadline: ${job.deadlineLabel}</span>
                        </div>
                        <div class="action-row section-gap">
                            <a class="primary-button small-button" href="#job-${job.jobId}">Jump to queue</a>
                            <a class="secondary-button small-button" href="${pageContext.request.contextPath}/jobs/detail?jobId=${job.jobId}">Open details</a>
                        </div>
                    </article>
                </c:forEach>
            </div>
        </section>
    </c:if>

    <section class="panel">
        <div class="section-head">
            <div>
                <h2>Create a new job</h2>
                <p class="muted-copy">Use comma or new line separators for skills. New jobs are persisted as JSON files immediately and appear in the review panels below.</p>
            </div>
            <span class="metric-pill">Lightweight text storage</span>
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
                <span>Quota</span>
                <input class="input" type="number" name="quota" min="1" step="1" placeholder="1" required />
            </label>
            <label class="field-group">
                <span>Workload hours</span>
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

    <c:forEach var="job" items="${jobs}">
        <section class="panel section-gap" id="job-${job.jobId}">
            <div class="section-head">
                <div>
                    <h2>${job.title}</h2>
                    <p class="muted-copy">${job.moduleCode} · Quota ${job.quota} · Workload ${job.workloadHours}h · ${job.deadlineStatusLabel}</p>
                </div>
                <div class="top-actions">
                    <span class="status-pill status-${job.status.cssClass}">${job.status.label}</span>
                    <form class="inline-form inline-form-tight" method="post" action="${pageContext.request.contextPath}/jobs/status">
                        <input type="hidden" name="jobId" value="${job.jobId}" />
                        <c:choose>
                            <c:when test="${job.open}">
                                <input type="hidden" name="status" value="CLOSED" />
                                <button class="secondary-button small-button" type="submit" data-loading-text="Closing...">Close job</button>
                            </c:when>
                            <c:otherwise>
                                <input type="hidden" name="status" value="OPEN" />
                                <button class="primary-button small-button" type="submit" data-loading-text="Reopening...">Reopen</button>
                            </c:otherwise>
                        </c:choose>
                    </form>
                    <a class="secondary-button small-button" href="${pageContext.request.contextPath}/jobs/detail?jobId=${job.jobId}">Open details</a>
                </div>
            </div>
            <p class="muted-block">${job.description}</p>
            <div class="job-meta-row section-gap">
                <span class="meta-chip">Required: ${job.requiredSkillsSummary}</span>
                <span class="meta-chip">Preferred: ${job.preferredSkillsSummary}</span>
                <span class="meta-chip">Deadline: ${job.deadlineLabel}</span>
                <span class="meta-chip">Applications: ${applicationsByJobId[job.jobId].size()}</span>
            </div>
            <div class="section-head section-gap">
                <div>
                    <h3>Candidate queue</h3>
                    <p class="muted-copy">Applicants are ranked by refreshed recommendation score first, then lighter workload.</p>
                </div>
                <span class="metric-pill refresh-pill" data-refresh-countdown>Refresh in ${autoRefreshSeconds}s</span>
            </div>
            <div class="table-wrapper section-gap">
                <table class="data-table">
                    <thead>
                    <tr>
                        <th>Applicant</th>
                        <th>Skills</th>
                        <th>Current workload</th>
                        <th>Recommendation</th>
                        <th>Status</th>
                        <th>Action</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="application" items="${applicationsByJobId[job.jobId]}">
                        <c:set var="candidate" value="${usersById[application.applicantId]}" />
                        <tr>
                            <td>
                                <strong>${candidate.name}</strong>
                                <div class="muted-copy">${candidate.studentId}</div>
                                <div class="muted-copy">${candidate.availability}</div>
                            </td>
                            <td>${candidate.skillsSummary}</td>
                            <td>
                                ${workloadByUserId[application.applicantId] == null ? 0 : workloadByUserId[application.applicantId]} / ${workloadThreshold} h
                            </td>
                            <td>
                                <strong>${application.recommendationPercent}%</strong>
                                <div class="muted-copy">${application.explanationSummary}</div>
                            </td>
                            <td><span class="status-pill status-${application.status.cssClass}">${application.status.label}</span></td>
                            <td>
                                <c:choose>
                                    <c:when test="${application.status.code == 'WITHDRAWN'}">
                                        <span class="muted-copy">Withdrawn by TA</span>
                                    </c:when>
                                    <c:otherwise>
                                        <form class="inline-form" method="post" action="${pageContext.request.contextPath}/applications/status">
                                            <input type="hidden" name="applicationId" value="${application.applicationId}" />
                                            <input type="hidden" name="jobId" value="${job.jobId}" />
                                            <select class="select" name="status">
                                                <option value="${application.status.code}">${application.status.label} (current)</option>
                                                <c:if test="${application.status.code != 'SUBMITTED'}"><option value="SUBMITTED">Submitted</option></c:if>
                                                <c:if test="${application.status.code != 'SHORTLISTED'}"><option value="SHORTLISTED">Shortlisted</option></c:if>
                                                <c:if test="${application.status.code != 'ACCEPTED'}"><option value="ACCEPTED">Accepted</option></c:if>
                                                <c:if test="${application.status.code != 'REJECTED'}"><option value="REJECTED">Rejected</option></c:if>
                                            </select>
                                            <button class="primary-button small-button" type="submit" data-loading-text="Updating...">Update</button>
                                        </form>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty applicationsByJobId[job.jobId]}">
                        <tr><td colspan="6" class="empty-state">No applications received yet for this job.</td></tr>
                    </c:if>
                    </tbody>
                </table>
            </div>
        </section>
    </c:forEach>
</div>
</body>
</html>
