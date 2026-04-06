<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>${job.title} · RecruitAssist</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css" />
    <script defer src="${pageContext.request.contextPath}/assets/js/app.js"></script>
</head>
<body data-auto-refresh-seconds="${autoRefreshSeconds}">
<div class="page-shell">
    <header class="hero-card home-hero dashboard-hero">
        <div>
            <div class="badge">Job Detail</div>
            <h1>${job.title}</h1>
            <p class="subtitle">${job.moduleCode} · Deadline ${job.deadline} (${job.deadlineStatusLabel}) · Owner ${ownerName}</p>
            <div class="hero-metrics">
                <div class="hero-metric"><strong>${applicationCount}</strong><span>Total applications</span></div>
                <div class="hero-metric"><strong>${acceptedCount}/${job.quota}</strong><span>Accepted / quota</span></div>
                <div class="hero-metric"><strong>${job.workloadHours} h</strong><span>Role workload</span></div>
            </div>
        </div>
        <aside class="spotlight-card">
            <div class="spotlight-kicker">Operational status</div>
            <div class="spotlight-score">${remainingQuota}<span> slots left</span></div>
            <h3>${job.status.label}</h3>
            <p class="muted-copy">This view auto-refreshes every ${autoRefreshSeconds} seconds when the page is idle, which helps MOs and Admins see status changes faster during reviews.</p>
            <div class="spotlight-meta">
                <span class="status-pill status-${job.status.cssClass}">${job.status.label}</span>
                <span class="metric-pill">${job.deadlineStatusLabel}</span>
                <span class="metric-pill refresh-pill" data-refresh-countdown>Refresh in ${autoRefreshSeconds}s</span>
            </div>
            <div class="action-row">
                <a class="secondary-button small-button" href="${pageContext.request.contextPath}/dashboard">Back to dashboard</a>
            </div>
        </aside>
    </header>

    <c:if test="${not empty flashMessage}">
        <div class="alert ${flashTone}">${flashMessage}</div>
    </c:if>

    <section class="kpi-grid">
        <article class="kpi-card">
            <span class="kpi-label">Applications</span>
            <strong>${applicationCount}</strong>
            <p>Total submitted records for this job.</p>
        </article>
        <article class="kpi-card">
            <span class="kpi-label">Accepted / quota</span>
            <strong>${acceptedCount} / ${job.quota}</strong>
            <p>${remainingQuota} slots are still available.</p>
        </article>
        <article class="kpi-card">
            <span class="kpi-label">Workload</span>
            <strong>${job.workloadHours} h</strong>
            <p>Expected accepted-hours contribution per assigned TA.</p>
        </article>
        <article class="kpi-card">
            <span class="kpi-label">Threshold</span>
            <strong>${workloadThreshold} h</strong>
            <p>Used by the recommendation engine when balancing workload.</p>
        </article>
    </section>

    <main class="dashboard-grid">
        <section class="panel">
            <div class="section-head">
                <h2>Role description</h2>
            </div>
            <div class="detail-pairs">
                <div class="detail-pair"><span>Module code</span><strong>${job.moduleCode}</strong></div>
                <div class="detail-pair"><span>Deadline</span><strong>${job.deadlineLabel} · ${job.deadlineStatusLabel}</strong></div>
                <div class="detail-pair"><span>Required skills</span><strong>${job.requiredSkillsSummary}</strong></div>
                <div class="detail-pair"><span>Preferred skills</span><strong>${job.preferredSkillsSummary}</strong></div>
                <div class="detail-pair full-width"><span>Description</span><strong>${job.description}</strong></div>
            </div>
        </section>

        <c:choose>
            <c:when test="${taView}">
                <section class="panel">
                    <div class="section-head">
                        <div>
                            <h2>Your match snapshot</h2>
                            <p class="muted-copy">${recommendation.fitLabel} based on live profile evidence, workload projection and competition pressure.</p>
                        </div>
                        <span class="score-badge">${recommendation.scorePercent}%</span>
                    </div>
                    <div class="job-meta-row">
                        <span class="meta-chip"><strong>${recommendation.actionLabel}</strong></span>
                        <span class="meta-chip">${recommendation.evidenceLabel}</span>
                        <span class="meta-chip">${recommendation.projectedWorkloadLabel}</span>
                        <span class="meta-chip">${recommendation.competitionSummary}</span>
                    </div>
                    <div class="detail-pairs compact-pairs section-gap">
                        <div class="detail-pair"><span>Matched skills</span><strong>${recommendation.matchedSkillsSummary}</strong></div>
                        <div class="detail-pair"><span>Preferred matched</span><strong>${recommendation.preferredMatchedSkillsSummary}</strong></div>
                        <div class="detail-pair"><span>Missing skills</span><strong>${recommendation.missingSkillsSummary}</strong></div>
                        <div class="detail-pair"><span>Status</span><strong>${existingApplication == null ? 'Not applied yet' : existingApplication.status.label}</strong></div>
                    </div>
                    <div class="surface-note section-gap">
                        <strong>Tip:</strong> Uploading a CV and keeping the evidence summary current usually improves explainability and makes missing-skill guidance more useful.
                    </div>
                    <div class="signal-grid section-gap">
                        <div class="signal-card">
                            <div class="signal-head"><span class="signal-label">Skill match</span><strong class="signal-value">${recommendation.skillScorePercent}%</strong></div>
                            <div class="progress-track"><div class="progress-fill progress-fill-brand" style="width:${recommendation.skillScorePercent}%"></div></div>
                            <div class="signal-note">Coverage of required and preferred skills.</div>
                        </div>
                        <div class="signal-card">
                            <div class="signal-head"><span class="signal-label">Availability</span><strong class="signal-value">${recommendation.availabilityScorePercent}%</strong></div>
                            <div class="progress-track"><div class="progress-fill progress-fill-success" style="width:${recommendation.availabilityScorePercent}%"></div></div>
                            <div class="signal-note">How actionable your availability note is for scheduling.</div>
                        </div>
                        <div class="signal-card">
                            <div class="signal-head"><span class="signal-label">Experience</span><strong class="signal-value">${recommendation.experienceScorePercent}%</strong></div>
                            <div class="progress-track"><div class="progress-fill progress-fill-brand" style="width:${recommendation.experienceScorePercent}%"></div></div>
                            <div class="signal-note">Role-relevant evidence found in experience and CV text.</div>
                        </div>
                        <div class="signal-card">
                            <div class="signal-head"><span class="signal-label">Workload</span><strong class="signal-value">${recommendation.workloadBalanceScorePercent}%</strong></div>
                            <div class="progress-track"><div class="progress-fill progress-fill-warning" style="width:${recommendation.workloadBalanceScorePercent}%"></div></div>
                            <div class="signal-note">${recommendation.workloadSummary}</div>
                        </div>
                        <div class="signal-card">
                            <div class="signal-head"><span class="signal-label">Profile evidence</span><strong class="signal-value">${recommendation.profileEvidenceScorePercent}%</strong></div>
                            <div class="progress-track"><div class="progress-fill progress-fill-success" style="width:${recommendation.profileEvidenceScorePercent}%"></div></div>
                            <div class="signal-note">Completeness and consistency across your profile.</div>
                        </div>
                        <div class="signal-card">
                            <div class="signal-head"><span class="signal-label">Competition</span><strong class="signal-value">${recommendation.competitionScorePercent}%</strong></div>
                            <div class="progress-track"><div class="progress-fill progress-fill-danger" style="width:${recommendation.competitionScorePercent}%"></div></div>
                            <div class="signal-note">Higher means the current candidate pressure is easier to manage.</div>
                        </div>
                    </div>
                    <ul class="reason-list section-gap">
                        <c:forEach var="reason" items="${recommendation.reasons}">
                            <li>${reason}</li>
                        </c:forEach>
                    </ul>
                    <div class="action-row section-gap">
                        <c:choose>
                            <c:when test="${existingApplication != null}">
                                <span class="status-pill status-${existingApplication.status.cssClass}">Applied · ${existingApplication.status.label}</span>
                                <c:if test="${canWithdrawApplication}">
                                    <form class="inline-form inline-form-tight" method="post" action="${pageContext.request.contextPath}/applications/withdraw">
                                        <input type="hidden" name="applicationId" value="${existingApplication.applicationId}" />
                                        <input type="hidden" name="jobId" value="${job.jobId}" />
                                        <input type="hidden" name="returnTo" value="detail" />
                                        <button class="secondary-button small-button" type="submit" data-loading-text="Withdrawing...">Withdraw application</button>
                                    </form>
                                </c:if>
                            </c:when>
                            <c:when test="${canApplyToJob}">
                                <form class="inline-form inline-form-tight" method="post" action="${pageContext.request.contextPath}/apply">
                                    <input type="hidden" name="jobId" value="${job.jobId}" />
                                    <input type="hidden" name="returnTo" value="detail" />
                                    <button class="primary-button" type="submit" data-loading-text="Submitting application...">Apply for this job</button>
                                </form>
                            </c:when>
                            <c:otherwise>
                                <span class="status-pill status-${job.status.cssClass}">${job.status.label}</span>
                                <span class="muted-copy">This role is currently closed for new applications.</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </section>
            </c:when>
            <c:when test="${canManage}">
                <section class="panel">
                    <div class="section-head">
                        <div>
                            <h2>Manage this job</h2>
                            <p class="muted-copy">Update the role details or pause applications without leaving this page.</p>
                        </div>
                        <span class="metric-pill">Owner controls</span>
                    </div>
                    <form class="form-grid two-column-form" method="post" action="${pageContext.request.contextPath}/jobs/update">
                        <input type="hidden" name="jobId" value="${job.jobId}" />
                        <input type="hidden" name="returnTo" value="detail" />
                        <input type="hidden" name="sort" value="${candidateSort}" />
                        <input type="hidden" name="filterStatus" value="${candidateStatusFilter}" />
                        <label class="field-group">
                            <span>Job title</span>
                            <input class="input" type="text" name="title" value="${job.title}" required />
                        </label>
                        <label class="field-group">
                            <span>Module code</span>
                            <input class="input" type="text" name="moduleCode" value="${job.moduleCode}" required />
                        </label>
                        <label class="field-group">
                            <span>Deadline</span>
                            <input class="input" type="date" name="deadline" value="${job.deadline}" required />
                        </label>
                        <label class="field-group">
                            <span>Quota</span>
                            <input class="input" type="number" name="quota" min="1" step="1" value="${job.quota}" required />
                        </label>
                        <label class="field-group">
                            <span>Workload hours</span>
                            <input class="input" type="number" name="workloadHours" min="1" step="1" value="${job.workloadHours}" required />
                        </label>
                        <div class="field-group full-width">
                            <span>Required skills</span>
                            <textarea class="textarea" name="requiredSkills" rows="3" required>${job.requiredSkillsSummary}</textarea>
                        </div>
                        <div class="field-group full-width">
                            <span>Preferred skills</span>
                            <textarea class="textarea" name="preferredSkills" rows="3">${job.preferredSkillsSummary}</textarea>
                        </div>
                        <div class="field-group full-width">
                            <span>Description</span>
                            <textarea class="textarea" name="description" rows="4" required>${job.description}</textarea>
                        </div>
                        <div class="form-actions full-width">
                            <button class="primary-button" type="submit" data-loading-text="Saving changes...">Save changes</button>
                        </div>
                    </form>
                    <div class="action-row section-gap">
                        <span class="status-pill status-${job.status.cssClass}">${job.status.label}</span>
                        <form class="inline-form inline-form-tight" method="post" action="${pageContext.request.contextPath}/jobs/status">
                            <input type="hidden" name="jobId" value="${job.jobId}" />
                            <input type="hidden" name="returnTo" value="detail" />
                            <input type="hidden" name="sort" value="${candidateSort}" />
                            <input type="hidden" name="filterStatus" value="${candidateStatusFilter}" />
                            <c:choose>
                                <c:when test="${job.open}">
                                    <input type="hidden" name="status" value="CLOSED" />
                                    <button class="secondary-button small-button" type="submit" data-loading-text="Closing applications...">Close applications</button>
                                </c:when>
                                <c:otherwise>
                                    <input type="hidden" name="status" value="OPEN" />
                                    <button class="primary-button small-button" type="submit" data-loading-text="Reopening applications...">Reopen applications</button>
                                </c:otherwise>
                            </c:choose>
                        </form>
                    </div>
                </section>
            </c:when>
            <c:otherwise>
                <section class="panel">
                    <div class="section-head">
                        <h2>Operational summary</h2>
                    </div>
                    <ul class="reason-list">
                        <li>This role is currently <strong>${job.status.label}</strong> with <strong>${remainingQuota}</strong> slots remaining.</li>
                        <li>Applications received so far: <strong>${applicationCount}</strong>, of which <strong>${acceptedCount}</strong> are accepted.</li>
                        <li>The workload threshold used by the balancing rule is <strong>${workloadThreshold}h</strong>.</li>
                        <c:if test="${not showApplications}">
                            <li>Applicant-level details are only visible to the job owner or an admin account.</li>
                        </c:if>
                    </ul>
                </section>
            </c:otherwise>
        </c:choose>
    </main>

    <c:if test="${showApplications}">
        <section class="panel section-gap">
            <div class="section-head">
                <div>
                    <h2>Applications for this job</h2>
                    <p class="muted-copy">${visibleApplicationCount} of ${applicationCount} applications shown in the current view.</p>
                </div>
                <span class="metric-pill refresh-pill" data-refresh-countdown>Refresh in ${autoRefreshSeconds}s</span>
            </div>
            <form class="inline-form section-gap" method="get" action="${pageContext.request.contextPath}/jobs/detail">
                <input type="hidden" name="jobId" value="${job.jobId}" />
                <select class="select" name="sort">
                    <option value="${candidateSort}">${candidateSortLabel} (current)</option>
                    <c:if test="${candidateSort != 'score'}"><option value="score">Best recommendation first</option></c:if>
                    <c:if test="${candidateSort != 'workload'}"><option value="workload">Lightest workload first</option></c:if>
                    <c:if test="${candidateSort != 'submitted'}"><option value="submitted">Newest submission first</option></c:if>
                    <c:if test="${candidateSort != 'status'}"><option value="status">Status priority</option></c:if>
                </select>
                <select class="select" name="filterStatus">
                    <option value="${candidateStatusFilter}">${candidateStatusFilterLabel} (current)</option>
                    <c:if test="${candidateStatusFilter != 'ALL'}"><option value="ALL">All statuses</option></c:if>
                    <c:if test="${candidateStatusFilter != 'SUBMITTED'}"><option value="SUBMITTED">Submitted</option></c:if>
                    <c:if test="${candidateStatusFilter != 'SHORTLISTED'}"><option value="SHORTLISTED">Shortlisted</option></c:if>
                    <c:if test="${candidateStatusFilter != 'ACCEPTED'}"><option value="ACCEPTED">Accepted</option></c:if>
                    <c:if test="${candidateStatusFilter != 'REJECTED'}"><option value="REJECTED">Rejected</option></c:if>
                    <c:if test="${candidateStatusFilter != 'WITHDRAWN'}"><option value="WITHDRAWN">Withdrawn</option></c:if>
                </select>
                <button class="secondary-button small-button" type="submit">Apply view</button>
            </form>
            <div class="table-wrapper section-gap">
                <table class="data-table">
                    <thead>
                    <tr>
                        <th>Applicant</th>
                        <th>Skills &amp; profile</th>
                        <th>Current workload</th>
                        <th>Recommendation</th>
                        <th>Status</th>
                        <th>CV</th>
                        <th>Action</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="application" items="${applications}">
                        <c:set var="candidate" value="${usersById[application.applicantId]}" />
                        <tr>
                            <td>
                                <strong>${candidate.name}</strong>
                                <div class="muted-copy">${candidate.studentId}</div>
                                <div class="muted-copy">${candidate.programme}</div>
                            </td>
                            <td>
                                <strong>${candidate.skillsSummary}</strong>
                                <div class="muted-copy">${candidate.availability}</div>
                                <div class="muted-copy">${candidate.experience}</div>
                            </td>
                            <td>${workloadByUserId[application.applicantId] == null ? 0 : workloadByUserId[application.applicantId]} / ${workloadThreshold} h</td>
                            <td>
                                <strong>${application.recommendationPercent}%</strong>
                                <div class="muted-copy">${application.explanationSummary}</div>
                            </td>
                            <td><span class="status-pill status-${application.status.cssClass}">${application.status.label}</span></td>
                            <td>
                                <c:choose>
                                    <c:when test="${candidate.cvAvailable}">
                                        <a class="inline-link" href="${pageContext.request.contextPath}/cv/download?userId=${candidate.userId}&jobId=${job.jobId}">Download CV</a>
                                        <div class="muted-copy">${candidate.cvUploadedAtLabel}</div>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="muted-copy">No CV uploaded</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${canReview and application.status.code != 'WITHDRAWN'}">
                                        <form class="inline-form" method="post" action="${pageContext.request.contextPath}/applications/status">
                                            <input type="hidden" name="applicationId" value="${application.applicationId}" />
                                            <input type="hidden" name="jobId" value="${job.jobId}" />
                                            <input type="hidden" name="returnTo" value="detail" />
                                            <input type="hidden" name="sort" value="${candidateSort}" />
                                            <input type="hidden" name="filterStatus" value="${candidateStatusFilter}" />
                                            <select class="select" name="status">
                                                <option value="${application.status.code}">${application.status.label} (current)</option>
                                                <c:if test="${application.status.code != 'SUBMITTED'}"><option value="SUBMITTED">Submitted</option></c:if>
                                                <c:if test="${application.status.code != 'SHORTLISTED'}"><option value="SHORTLISTED">Shortlisted</option></c:if>
                                                <c:if test="${application.status.code != 'ACCEPTED'}"><option value="ACCEPTED">Accepted</option></c:if>
                                                <c:if test="${application.status.code != 'REJECTED'}"><option value="REJECTED">Rejected</option></c:if>
                                            </select>
                                            <button class="primary-button small-button" type="submit" data-loading-text="Updating...">Update</button>
                                        </form>
                                    </c:when>
                                    <c:when test="${application.status.code == 'WITHDRAWN'}">
                                        <span class="muted-copy">Withdrawn by TA</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="muted-copy">View only</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty applications and applicationCount > 0}">
                        <tr><td colspan="7" class="empty-state">No applications match the current sort and status filter.</td></tr>
                    </c:if>
                    <c:if test="${empty applications and applicationCount == 0}">
                        <tr><td colspan="7" class="empty-state">No applications have been submitted for this job yet.</td></tr>
                    </c:if>
                    </tbody>
                </table>
            </div>
        </section>
    </c:if>
</div>
</body>
</html>
