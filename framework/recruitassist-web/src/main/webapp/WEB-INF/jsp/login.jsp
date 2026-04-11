<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Login · RecruitAssist</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css" />
    <script defer src="${pageContext.request.contextPath}/assets/js/app.js"></script>
</head>
<body>
<div class="page-shell narrow-shell">
    <header class="hero-card compact-hero">
        <div class="badge">Demo access</div>
        <h1>Sign in to RecruitAssist</h1>
        <p class="subtitle">The system is now seeded for higher-load demos. We keep the login surface compact by showing only featured accounts below.</p>
        <div class="hero-metrics section-gap">
            <div class="hero-metric"><strong>${totalUserCount}</strong><span>Total demo accounts</span></div>
            <div class="hero-metric"><strong>${taCount}</strong><span>Teaching assistants</span></div>
            <div class="hero-metric"><strong>${moCount}</strong><span>Recruiters / MOs</span></div>
            <div class="hero-metric"><strong>${adminCount}</strong><span>Admin accounts</span></div>
        </div>
    </header>

    <c:if test="${not empty flashMessage}">
        <div class="alert ${flashTone}">${flashMessage}</div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert error">${error}</div>
    </c:if>

    <main class="content-grid login-layout">
        <section class="panel login-card">
            <div class="section-head">
                <div>
                    <h2>Login form</h2>
                    <p class="muted-copy">Quick-fill a featured seeded account on the right, then sign in with one click.</p>
                </div>
                <span class="metric-pill">Fast demo entry</span>
            </div>
            <form class="form-grid" method="post" action="${pageContext.request.contextPath}/login">
                <label class="field-group">
                    <span>Username</span>
                    <input class="input" type="text" name="username" value="${username}" placeholder="e.g. alice.ta" required data-login-username />
                </label>
                <label class="field-group">
                    <span>Password</span>
                    <input class="input" type="password" name="password" placeholder="${demoPassword}" required data-login-password />
                </label>
                <div class="surface-note">
                    <strong>Tip:</strong> click any <em>Use account</em> button to prefill the form. All generated demo accounts still use <code>${demoPassword}</code>.
                </div>
                <button class="primary-button" type="submit" data-loading-text="Signing in...">Sign in</button>
                <a class="secondary-button" href="${pageContext.request.contextPath}/register">Create a new account</a>
                <a class="secondary-button" href="${pageContext.request.contextPath}/home">Back to home</a>
            </form>
        </section>

        <section class="panel">
            <div class="section-head">
                <div>
                    <h2>Featured demo accounts</h2>
                    <p class="muted-copy">Only a curated subset is shown here so the page stays clean even when the seed dataset is much larger.</p>
                </div>
                <span class="metric-pill">Password: ${demoPassword}</span>
            </div>
            <div class="table-wrapper">
                <table class="data-table compact-table">
                    <thead>
                    <tr>
                        <th>Name</th>
                        <th>Role</th>
                        <th>Username</th>
                        <th>Quick fill</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="demoUser" items="${featuredDemoUsers}">
                        <tr>
                            <td>
                                <strong>${demoUser.name}</strong>
                                <div class="muted-copy">${demoUser.programme}</div>
                            </td>
                            <td><span class="status-pill status-${demoUser.role.cssClass}">${demoUser.role.label}</span></td>
                            <td><code>${demoUser.username}</code></td>
                            <td>
                                <button
                                        class="secondary-button small-button"
                                        type="button"
                                        data-fill-login
                                        data-username="${demoUser.username}"
                                        data-password="${demoPassword}">
                                    Use account
                                </button>
                            </td>
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
