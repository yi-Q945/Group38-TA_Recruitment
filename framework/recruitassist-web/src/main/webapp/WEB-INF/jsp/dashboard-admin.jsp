<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Admin Dashboard · RecruitAssist</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css" />
    <script defer src="${pageContext.request.contextPath}/assets/js/app.js"></script>
</head>
<body data-auto-refresh-seconds="${autoRefreshSeconds}">
<div class="page-shell">
    <header class="hero-card home-hero dashboard-hero">
        <div>
            <div class="badge">Admin Dashboard</div>
            <h1>${user.name}</h1>
            <p class="subtitle">Monitor workload balance, scan the whole recruitment pipeline, and keep an eye on job and application counts with a more operational overview.</p>
            <div class="hero-metrics">
                <div class="hero-metric"><strong>${jobCount}</strong><span>Total jobs</span></div>
                <div class="hero-metric"><strong>${openJobTotal}</strong><span>Currently open</span></div>
                <div class="hero-metric"><strong>${visibleApplicantCount}</strong><span>Applicants in current view</span></div>
            </div>
        </div>
        <aside class="spotlight-card">
            <div class="spotlight-kicker">Live board</div>
            <div class="spotlight-score">${visibleJobCount}<span> visible jobs</span></div>
            <h3>Operational recruitment monitor</h3>
            <p class="muted-copy">This page auto-refreshes every ${autoRefreshSeconds} seconds when you are not editing filters, so status changes become visible faster during demos.</p>
            <div class="spotlight-meta">
                <span class="metric-pill">Threshold ${workloadThreshold} h</span>
                <span class="metric-pill">${jobStatusFilterLabel}</span>
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
            <span class="kpi-label">Tracked TAs</span>
            <strong>${workloadEntries.size()}</strong>
            <p>All seeded teaching assistants are included in the workload balance view.</p>
        </article>
        <article class="kpi-card">
            <span class="kpi-label">Recent applications</span>
            <strong>${latestApplications.size()}</strong>
            <p>Latest application activity is pulled directly from text-based records.</p>
        </article>
        <article class="kpi-card">
            <span class="kpi-label">Policy threshold</span>
            <strong>${workloadThreshold} h</strong>
            <p>Accepted hours above this line should be treated as overload risk.</p>
        </article>
        <article class="kpi-card">
            <span class="kpi-label">Filtered jobs</span>
            <strong>${visibleJobCount}</strong>
            <p>The recruitment overview below reacts to the current status and module filters.</p>
        </article>
    </section>

    <section class="panel">
        <div class="section-head">
            <div>
                <h2>Recruitment overview</h2>
                <p class="muted-copy">This fills the backlog gap for an admin-level overview of positions, applicants and job status.</p>
            </div>
            <span class="metric-pill">Current filter: ${jobStatusFilterLabel}</span>
        </div>

        <form class="filter-bar" method="get" action="${pageContext.request.contextPath}/dashboard">
            <label class="field-group compact-field">
                <span>Module or keyword</span>
                <input class="input" type="text" name="module" value="${moduleQuery}" placeholder="Search by module code, title or skill" />
            </label>
            <label class="field-group compact-field">
                <span>Status</span>
                <select class="select" name="jobStatus">
                    <option value="${jobStatusFilter}">${jobStatusFilterLabel} (current)</option>
                    <c:if test="${jobStatusFilter != 'ALL'}"><option value="ALL">All jobs</option></c:if>
                    <c:if test="${jobStatusFilter != 'OPEN'}"><option value="OPEN">Open jobs only</option></c:if>
                    <c:if test="${jobStatusFilter != 'CLOSED'}"><option value="CLOSED">Closed jobs only</option></c:if>
                </select>
            </label>
            <div class="form-actions compact-actions">
                <button class="secondary-button small-button" type="submit">Apply filters</button>
                <a class="secondary-button small-button" href="${pageContext.request.contextPath}/dashboard">Clear filters</a>
            </div>
        </form>

        <div class="table-wrapper section-gap">
            <table class="data-table">
                <thead>
                <tr>
                    <th>Job</th>
                    <th>Owner</th>
                    <th>Deadline</th>
                    <th>Applicants</th>
                    <th>Accepted / quota</th>
                    <th>Status</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="job" items="${adminJobs}">
                    <tr>
                        <td>
                            <strong>
                                <a class="inline-link" href="${pageContext.request.contextPath}/jobs/detail?jobId=${job.jobId}">
                                    ${job.title}
                                </a>
                            </strong>
                            <div class="muted-copy">${job.moduleCode}</div>
                        </td>
                        <td>${usersById[job.ownerId].name}</td>
                        <td>
                            <strong>${job.deadlineLabel}</strong>
                            <div class="muted-copy">${job.deadlineStatusLabel}</div>
                        </td>
                        <td>${applicationsByJobId[job.jobId].size()}</td>
                        <td>${acceptedByJobId[job.jobId]} / ${job.quota}</td>
                        <td><span class="status-pill status-${job.status.cssClass}">${job.status.label}</span></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty adminJobs}">
                    <tr><td colspan="6" class="empty-state">No jobs match the current admin filter.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </section>

    <main class="dashboard-grid section-gap">
        <section class="panel">
            <div class="section-head">
                <h2>TA workload overview</h2>
            </div>
            <div class="table-wrapper">
                <table class="data-table">
                    <thead>
                    <tr>
                        <th>TA</th>
                        <th>Accepted hours</th>
                        <th>Active applications</th>
                        <th>Status</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="entry" items="${workloadEntries}">
                        <tr>
                            <td>
                                <strong>${entry.user.name}</strong>
                                <div class="muted-copy">${entry.user.programme}</div>
                            </td>
                            <td>${entry.acceptedHours} / ${entry.threshold} h</td>
                            <td>${entry.activeApplications}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${entry.overloaded}">
                                        <span class="status-pill status-rejected">Over threshold</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="status-pill status-accepted">Balanced</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="panel">
            <div class="section-head">
                <h2>Latest applications</h2>
                <span class="metric-pill refresh-pill" data-refresh-countdown>Refresh in ${autoRefreshSeconds}s</span>
            </div>
            <div class="table-wrapper">
                <table class="data-table compact-table">
                    <thead>
                    <tr>
                        <th>Applicant</th>
                        <th>Job</th>
                        <th>Status</th>
                        <th>Score</th>
                        <th>Submitted</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="application" items="${latestApplications}">
                        <tr>
                            <td>${usersById[application.applicantId].name}</td>
                            <td>
                                <a class="inline-link" href="${pageContext.request.contextPath}/jobs/detail?jobId=${application.jobId}">
                                    ${jobsById[application.jobId].title}
                                </a>
                            </td>
                            <td><span class="status-pill status-${application.status.cssClass}">${application.status.label}</span></td>
                            <td>${application.recommendationPercent}%</td>
                            <td>${application.applyTimeLabel}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </section>
    </main>
</div>
</body>
</html>
