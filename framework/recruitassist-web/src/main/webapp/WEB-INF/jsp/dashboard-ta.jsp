<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>TA Dashboard · RecruitAssist</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css?v=302" />
    <script defer src="${pageContext.request.contextPath}/assets/js/app.js?v=302"></script>
</head>
<body>
<div class="page-shell">
    <header class="hero-card home-hero dashboard-hero">
        <div>
            <div class="badge">TA Dashboard</div>
            <h1>Welcome back, ${user.name}</h1>
            <p class="subtitle">Keep your profile evidence fresh, upload your CV, and focus on the highest-value roles with searchable explainable recommendations.</p>
            <div class="hero-metrics">
                <div class="hero-metric"><strong>${currentWorkload}/${workloadThreshold} h</strong><span>Current accepted workload</span></div>
                <div class="hero-metric"><strong>${activeApplicationCount}</strong><span>Active applications</span></div>
                <div class="hero-metric"><strong>${profileSignalPercent}%</strong><span>Profile evidence readiness</span></div>
            </div>
        </div>
        <c:choose>
            <c:when test="${topRecommendation != null}">
                <aside class="spotlight-card">
                    <div class="spotlight-kicker">Top recommendation</div>
                    <div class="spotlight-score">${topRecommendation.scorePercent}<span>% match</span></div>
                    <h3>${topRecommendation.job.title}</h3>
                    <p class="muted-copy">${topRecommendation.job.moduleCode} · ${topRecommendation.fitLabel} · ${topRecommendation.actionLabel}</p>
                    <div class="spotlight-meta">
                        <span class="metric-pill">${topRecommendation.evidenceLabel}</span>
                        <span class="metric-pill">${topRecommendation.competitionSummary}</span>
                        <span class="metric-pill">${topRecommendation.job.deadlineStatusLabel}</span>
                    </div>
                    <div class="action-row">
                        <a class="secondary-button small-button" href="${pageContext.request.contextPath}/jobs/detail?jobId=${topRecommendation.job.jobId}">Open top job</a>
                        <a class="secondary-button small-button" href="${pageContext.request.contextPath}/logout">Log out</a>
                    </div>
                </aside>
            </c:when>
            <c:otherwise>
                <aside class="spotlight-card">
                    <div class="spotlight-kicker">No open roles</div>
                    <div class="spotlight-score">0<span> live matches</span></div>
                    <h3>Nothing to apply for right now</h3>
                    <p class="muted-copy">Once more roles are published, this panel will highlight the best opportunity based on your profile evidence and workload balance.</p>
                    <div class="action-row">
                        <a class="secondary-button small-button" href="${pageContext.request.contextPath}/logout">Log out</a>
                    </div>
                </aside>
            </c:otherwise>
        </c:choose>
    </header>

    <c:if test="${not empty flashMessage}">
        <div class="alert ${flashTone}">${flashMessage}</div>
    </c:if>

    <section class="kpi-grid">
        <article class="kpi-card">
            <span class="kpi-label">Role</span>
            <strong>${user.role.label}</strong>
            <p>${user.programme}</p>
        </article>
        <article class="kpi-card">
            <span class="kpi-label">Skills</span>
            <strong>${user.skills.size()}</strong>
            <p>${user.skillsSummary}</p>
        </article>
        <article class="kpi-card">
            <span class="kpi-label">Accepted offers</span>
            <strong>${acceptedApplicationCount}</strong>
            <p>Accepted applications already contributing to your committed workload.</p>
        </article>
        <article class="kpi-card">
            <span class="kpi-label">Profile readiness</span>
            <strong><c:choose><c:when test="${profileReady}">Ready</c:when><c:otherwise>Needs update</c:otherwise></c:choose></strong>
            <p>Applications work best when skills, availability, experience and CV evidence are up to date.</p>
        </article>
    </section>

    <!-- ===== RECOMMENDED JOBS (primary content — shown first) ===== -->
    <section class="panel section-gap">
        <div class="section-head">
            <div>
                <h2>Browse &amp; apply for TA positions</h2>
                <p class="muted-copy">Search, filter, and apply for open positions. Each card shows your personalised match score and explanation.</p>
            </div>
            <span class="metric-pill">${recommendedJobs.size()} jobs · ${jobSortLabel}</span>
        </div>

        <form class="filter-bar section-gap" method="get" action="${pageContext.request.contextPath}/dashboard">
            <label class="field-group compact-field">
                <span>Search jobs</span>
                <input class="input" type="text" name="q" value="${jobSearchQuery}" placeholder="Search by module, skill or keyword" />
            </label>
            <label class="field-group compact-field">
                <span>Filter by skill</span>
                <input class="input" type="text" name="skillFilter" value="${skillFilter}" placeholder="e.g. Python, Java" />
            </label>
            <label class="field-group compact-field">
                <span>Max hours/week</span>
                <input class="input" type="number" name="maxHours" value="${maxHours}" placeholder="e.g. 8" min="1" max="40" />
            </label>
            <label class="field-group compact-field">
                <span>Deadline before</span>
                <input class="input" type="date" name="deadlineBefore" value="${deadlineBefore}" />
            </label>
            <label class="field-group compact-field">
                <span>Sort by</span>
                <select class="select" name="sort">
                    <option value="${jobSort}">${jobSortLabel} (current)</option>
                    <c:if test="${jobSort != 'score'}"><option value="score">Best match first</option></c:if>
                    <c:if test="${jobSort != 'deadline'}"><option value="deadline">Closest deadline first</option></c:if>
                    <c:if test="${jobSort != 'workload'}"><option value="workload">Lowest projected workload first</option></c:if>
                </select>
            </label>
            <div class="form-actions compact-actions">
                <button class="secondary-button small-button" type="submit">Apply filters</button>
                <a class="secondary-button small-button" href="${pageContext.request.contextPath}/dashboard">Clear filters</a>
            </div>
        </form>

        <div class="card-grid section-gap">
            <c:forEach var="recommendation" items="${recommendedJobs}">
                <c:set var="existingApplication" value="${applicationsByJobId[recommendation.job.jobId]}" />
                <article class="job-card">
                    <div class="job-card-header">
                        <div>
                            <h3>
                                <a class="inline-link" href="${pageContext.request.contextPath}/jobs/detail?jobId=${recommendation.job.jobId}">
                                    ${recommendation.job.title}
                                </a>
                            </h3>
                            <p class="muted-copy">${recommendation.job.moduleCode} · Deadline ${recommendation.job.deadlineLabel} · ${recommendation.job.deadlineStatusLabel}</p>
                        </div>
                        <div class="top-actions">
                            <span class="metric-pill">${recommendation.fitLabel}</span>
                            <span class="score-badge">${recommendation.scorePercent}%</span>
                        </div>
                    </div>

                    <div class="job-meta-row">
                        <span class="meta-chip"><strong>${recommendation.actionLabel}</strong></span>
                        <span class="meta-chip">${recommendation.job.workloadHours}h role load</span>
                        <span class="meta-chip">${recommendation.projectedWorkloadLabel}</span>
                        <span class="meta-chip">${recommendation.competitionSummary}</span>
                    </div>

                    <div class="detail-pairs compact-pairs">
                        <div class="detail-pair"><span>Required</span><strong>${recommendation.job.requiredSkillsSummary}</strong></div>
                        <div class="detail-pair"><span>Preferred</span><strong>${recommendation.job.preferredSkillsSummary}</strong></div>
                        <div class="detail-pair"><span>Matched</span><strong>${recommendation.matchedSkillsSummary}</strong></div>
                        <div class="detail-pair"><span>Preferred matched</span><strong>${recommendation.preferredMatchedSkillsSummary}</strong></div>
                        <div class="detail-pair full-width"><span>Missing</span><strong>${recommendation.missingSkillsSummary}</strong></div>
                    </div>

                    <div class="signal-grid section-gap">
                        <div class="signal-card">
                            <div class="signal-head"><span class="signal-label">Skill match</span><strong class="signal-value">${recommendation.skillScorePercent}%</strong></div>
                            <div class="progress-track"><div class="progress-fill progress-fill-brand" style="width:${recommendation.skillScorePercent}%"></div></div>
                            <div class="signal-note">Required and preferred skill coverage.</div>
                        </div>
                        <div class="signal-card">
                            <div class="signal-head"><span class="signal-label">Availability</span><strong class="signal-value">${recommendation.availabilityScorePercent}%</strong></div>
                            <div class="progress-track"><div class="progress-fill progress-fill-success" style="width:${recommendation.availabilityScorePercent}%"></div></div>
                            <div class="signal-note">How usable your stated availability is for planning.</div>
                        </div>
                        <div class="signal-card">
                            <div class="signal-head"><span class="signal-label">Experience</span><strong class="signal-value">${recommendation.experienceScorePercent}%</strong></div>
                            <div class="progress-track"><div class="progress-fill progress-fill-brand" style="width:${recommendation.experienceScorePercent}%"></div></div>
                            <div class="signal-note">Role-relevant evidence found in experience and CV text.</div>
                        </div>
                        <div class="signal-card">
                            <div class="signal-head"><span class="signal-label">Workload balance</span><strong class="signal-value">${recommendation.workloadBalanceScorePercent}%</strong></div>
                            <div class="progress-track"><div class="progress-fill progress-fill-warning" style="width:${recommendation.workloadBalanceScorePercent}%"></div></div>
                            <div class="signal-note">Projected load if this application succeeds.</div>
                        </div>
                        <div class="signal-card">
                            <div class="signal-head"><span class="signal-label">Profile evidence</span><strong class="signal-value">${recommendation.profileEvidenceScorePercent}%</strong></div>
                            <div class="progress-track"><div class="progress-fill progress-fill-success" style="width:${recommendation.profileEvidenceScorePercent}%"></div></div>
                            <div class="signal-note">Completeness and consistency of your profile.</div>
                        </div>
                        <div class="signal-card">
                            <div class="signal-head"><span class="signal-label">Competition</span><strong class="signal-value">${recommendation.competitionScorePercent}%</strong></div>
                            <div class="progress-track"><div class="progress-fill progress-fill-danger" style="width:${recommendation.competitionScorePercent}%"></div></div>
                            <div class="signal-note">A higher score means the current demand pressure is more manageable.</div>
                        </div>
                    </div>

                    <ul class="reason-list section-gap">
                        <c:forEach var="reason" items="${recommendation.reasons}">
                            <li>${reason}</li>
                        </c:forEach>
                    </ul>

                    <div class="action-row">
                        <a class="secondary-button small-button" href="${pageContext.request.contextPath}/jobs/detail?jobId=${recommendation.job.jobId}">View details</a>
                        <c:choose>
                            <c:when test="${existingApplication != null}">
                                <span class="status-pill status-${existingApplication.status.cssClass}">Applied · ${existingApplication.status.label}</span>
                            </c:when>
                            <c:otherwise>
                                <form class="inline-form inline-form-tight" method="post" action="${pageContext.request.contextPath}/apply">
                                    <input type="hidden" name="jobId" value="${recommendation.job.jobId}" />
                                    <button class="primary-button small-button" type="submit" data-loading-text="Submitting application...">Apply now</button>
                                </form>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </article>
            </c:forEach>
        </div>
        <c:if test="${empty recommendedJobs}">
            <div class="empty-state large-empty">No open jobs match your current search or filter.</div>
        </c:if>
    </section>

    <!-- ===== APPLICATION HISTORY ===== -->
    <section class="panel section-gap">
        <div class="section-head">
            <h2>My applications</h2>
            <span class="metric-pill">${applications.size()} total</span>
        </div>
        <div class="table-wrapper">
            <table class="data-table compact-table">
                <thead><tr><th>Job</th><th>Status</th><th>Score</th><th>Submitted</th><th>Action</th></tr></thead>
                <tbody>
                <c:forEach var="application" items="${applications}">
                    <c:set var="applicationJob" value="${jobsById[application.jobId]}" />
                    <tr>
                        <td><strong><a class="inline-link" href="${pageContext.request.contextPath}/jobs/detail?jobId=${application.jobId}">${applicationJob.title}</a></strong><div class="muted-copy">${applicationJob.moduleCode}</div></td>
                        <td><span class="status-pill status-${application.status.cssClass}">${application.status.label}</span></td>
                        <td>${application.recommendationPercent}%</td>
                        <td>${application.applyTimeLabel}</td>
                        <td>
                            <c:choose>
                                <c:when test="${application.status.code != 'WITHDRAWN' and application.status.code != 'REJECTED'}">
                                    <form class="inline-form inline-form-tight" method="post" action="${pageContext.request.contextPath}/applications/withdraw">
                                        <input type="hidden" name="applicationId" value="${application.applicationId}" />
                                        <input type="hidden" name="jobId" value="${application.jobId}" />
                                        <button class="secondary-button small-button" type="submit" data-loading-text="Withdrawing...">Withdraw</button>
                                    </form>
                                </c:when>
                                <c:otherwise><span class="muted-copy">No action</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty applications}"><tr><td colspan="5" class="empty-state">You have not applied yet. Browse the positions above and click Apply!</td></tr></c:if>
                </tbody>
            </table>
        </div>
    </section>

    <!-- ===== PROFILE & CV ===== -->
    <section class="panel section-gap">
        <div class="section-head">
            <div><h2>Profile &amp; CV management</h2><p class="muted-copy">Keep your profile updated for better recommendation scores.</p></div>
            <span class="status-pill status-ta"><c:choose><c:when test="${profileReady}">Profile ready</c:when><c:otherwise>Update required</c:otherwise></c:choose></span>
        </div>
        <form class="form-grid two-column-form section-gap" method="post" action="${pageContext.request.contextPath}/profile/update">
            <label class="field-group"><span>Full name</span><input class="input" type="text" name="name" value="${user.name}" required /></label>
            <label class="field-group"><span>Student ID</span><input class="input" type="text" name="studentId" value="${user.studentId}" required /></label>
            <label class="field-group"><span>Email</span><input class="input" type="email" name="email" value="${user.email}" /></label>
            <label class="field-group"><span>Programme</span><input class="input" type="text" name="programme" value="${user.programme}" required /></label>
            <div class="field-group full-width"><span>Skills</span><textarea class="textarea" name="skills" rows="2" required>${user.skillsSummary}</textarea></div>
            <div class="field-group full-width"><span>Availability</span><textarea class="textarea" name="availability" rows="2" required>${user.availability}</textarea></div>
            <div class="field-group full-width"><span>Experience</span><textarea class="textarea" name="experience" rows="3">${user.experience}</textarea></div>
            <div class="field-group full-width"><span>CV text for AI matching</span><textarea class="textarea" name="cvText" rows="3">${user.cvText}</textarea></div>
            <div class="form-actions full-width"><button class="primary-button" type="submit" data-loading-text="Saving...">Save profile</button></div>
        </form>
        <form class="form-grid section-gap" method="post" action="${pageContext.request.contextPath}/profile/cv/upload" enctype="multipart/form-data">
            <label class="upload-zone" data-upload-zone>
                <span class="upload-zone-title">Upload CV file</span>
                <span class="upload-zone-hint" data-upload-filename>Drop file here or choose from device</span>
                <span class="upload-zone-meta" data-upload-hint>PDF, DOC, DOCX, TXT · max 5MB</span>
                <span class="secondary-button small-button upload-trigger">Choose file</span>
                <input class="upload-input" type="file" name="cvFile" accept=".pdf,.doc,.docx,.txt" data-max-mb="5" required />
            </label>
            <div class="action-row"><button class="secondary-button" type="submit" data-loading-text="Uploading...">Upload CV</button></div>
        </form>
        <c:if test="${user.cvAvailable}">
            <div class="surface-note section-gap" style="display:flex;align-items:center;gap:12px;flex-wrap:wrap;">
                <strong>Current CV:</strong> <code>${user.cvFileLabel}</code>
                <span class="muted-copy">Uploaded ${user.cvUploadedAtLabel}</span>
                <a class="primary-button small-button" href="${pageContext.request.contextPath}/cv/download?userId=${user.userId}">Download CV</a>
            </div>
        </c:if>
        <c:if test="${not user.cvAvailable}">
            <div class="surface-note section-gap"><strong>No CV uploaded yet.</strong> Upload a file to boost your score.</div>
        </c:if>
    </section>
</div>
</body>
</html>
