<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>${appName}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css" />
    <script defer src="${pageContext.request.contextPath}/assets/js/app.js"></script>
</head>
<body>
<div class="page-shell">
    <header class="hero-card home-hero dashboard-hero">
        <div>
            <div class="badge">Course-ready Java Web Prototype</div>
            <h1>${appName}</h1>
            <p class="subtitle">A polished Servlet/JSP prototype for the Teaching Assistant Recruitment System, with explainable recommendations, operational dashboards and demo-ready seeded data.</p>
            <p class="stack">${stack}</p>
            <div class="hero-metrics">
                <div class="hero-metric"><strong>${taCount}</strong><span>TA accounts ready</span></div>
                <div class="hero-metric"><strong>${recruiterCount}</strong><span>Recruiters / MOs</span></div>
                <div class="hero-metric"><strong>${adminCount}</strong><span>Admin accounts</span></div>
                <div class="hero-metric"><strong>${jobCount}</strong><span>Jobs in seeded view</span></div>
                <div class="hero-metric"><strong>${applicationCount}</strong><span>Applications for load demos</span></div>
            </div>
            <div class="hero-actions section-gap">
                <a class="primary-button" href="${pageContext.request.contextPath}/login">Open demo login</a>
                <div class="hero-note">All seeded demo accounts use password <strong>${demoPassword}</strong>.</div>
            </div>
        </div>
        <aside class="spotlight-card">
            <div class="spotlight-kicker">Demo-ready experience</div>
            <div class="spotlight-score">${taCount + recruiterCount + adminCount}<span> seeded users</span></div>
            <h3>Now suitable for broader workflow checks</h3>
            <p class="muted-copy">The current build now supports larger seeded workloads, so you can inspect recommendation stability, recruiter queue behaviour and admin monitoring under denser demo traffic.</p>
            <div class="spotlight-meta">
                <span class="metric-pill">Explainable ranking</span>
                <span class="metric-pill">Unified detail view</span>
                <span class="metric-pill">Higher-load seed set</span>
            </div>
        </aside>
    </header>

    <section class="showcase-grid">
        <article class="feature-card">
            <div class="spotlight-kicker">TA workflow</div>
            <h3>Find the best-fit jobs faster</h3>
            <p class="muted-copy">Recommendations now combine skill coverage, profile evidence, workload projection and competition pressure into a single explainable score.</p>
        </article>
        <article class="feature-card">
            <div class="spotlight-kicker">MO workflow</div>
            <h3>Manage roles without context switching</h3>
            <p class="muted-copy">Publish, edit, close, reopen and review candidates from the same role-aware dashboard with cleaner summaries.</p>
        </article>
        <article class="feature-card">
            <div class="spotlight-kicker">Admin workflow</div>
            <h3>Monitor workload risk clearly</h3>
            <p class="muted-copy">Workload thresholds and recent application signals remain visible for operational checks and demo explanation.</p>
        </article>
    </section>

    <c:if test="${not empty featuredDemoUsers}">
        <section class="panel">
            <div class="section-head">
                <div>
                    <h2>Quick demo sign-in</h2>
                    <p class="muted-copy">Use one curated account per role to jump directly into TA, Recruiter and Admin flows from the home page.</p>
                </div>
                <span class="metric-pill">Password: ${demoPassword}</span>
            </div>
            <div class="showcase-grid">
                <c:forEach var="demoUser" items="${featuredDemoUsers}">
                    <article class="feature-card">
                        <div class="section-head">
                            <div>
                                <div class="spotlight-kicker">${demoUser.role.label} access</div>
                                <h3>${demoUser.name}</h3>
                            </div>
                            <span class="status-pill status-${demoUser.role.cssClass}">${demoUser.role.label}</span>
                        </div>
                        <p class="muted-copy">${demoUser.programme}</p>
                        <div class="job-meta-row section-gap">
                            <span class="meta-chip">Username: <code>${demoUser.username}</code></span>
                            <span class="meta-chip">Password: <code>${demoPassword}</code></span>
                        </div>
                        <form class="action-row section-gap" method="post" action="${pageContext.request.contextPath}/login">
                            <input type="hidden" name="username" value="${demoUser.username}" />
                            <input type="hidden" name="password" value="${demoPassword}" />
                            <button class="primary-button small-button" type="submit" data-loading-text="Signing in...">Quick sign in</button>
                            <a class="secondary-button small-button" href="${pageContext.request.contextPath}/login">Open login page</a>
                        </form>
                    </article>
                </c:forEach>
            </div>
        </section>
    </c:if>

    <main class="content-grid dashboard-grid single-column-grid">
        <section class="panel">
            <div class="section-head">
                <h2>Project layout</h2>
                <span class="status-pill status-open">Ready</span>
            </div>
            <ul class="info-list">
                <li><strong>Framework</strong><span>${frameworkDir}</span></li>
                <li><strong>Data</strong><span>${dataDir}</span></li>
                <li><strong>Logs</strong><span>${logsDir}</span></li>
            </ul>
        </section>

        <section class="panel">
            <div class="section-head">
                <h2>What is already implemented</h2>
                <span class="metric-pill">Current demo scope</span>
            </div>
            <div class="chip-row">
                <span class="chip">Seeded demo accounts</span>
                <span class="chip">Role-aware login</span>
                <span class="chip">Explainable TA recommendations</span>
                <span class="chip">Application submission</span>
                <span class="chip">TA application withdrawal</span>
                <span class="chip">MO job publishing</span>
                <span class="chip">MO job editing &amp; closure</span>
                <span class="chip">Unified job detail page</span>
                <span class="chip">Candidate sorting &amp; review</span>
                <span class="chip">Admin workload view</span>
            </div>
        </section>

        <section class="panel">
            <div class="section-head">
                <h2>Suggested demo path</h2>
                <span class="metric-pill">3 quick steps</span>
            </div>
            <div class="journey-grid">
                <article class="journey-step">
                    <div class="spotlight-kicker">Step 1</div>
                    <h3>Login as a TA</h3>
                    <p class="muted-copy">Open the recommendation dashboard, inspect the multi-signal fit score and apply to a role from a denser seeded pool.</p>
                </article>
                <article class="journey-step">
                    <div class="spotlight-kicker">Step 2</div>
                    <h3>Switch to an MO</h3>
                    <p class="muted-copy">Show job publishing, candidate ranking and the ability to update or close a posting from the same workspace.</p>
                </article>
                <article class="journey-step">
                    <div class="spotlight-kicker">Step 3</div>
                    <h3>Finish as Admin</h3>
                    <p class="muted-copy">Explain how workload thresholds and recent applications support fairer allocation decisions at a larger demo scale.</p>
                </article>
            </div>
        </section>
    </main>
</div>
</body>
</html>
