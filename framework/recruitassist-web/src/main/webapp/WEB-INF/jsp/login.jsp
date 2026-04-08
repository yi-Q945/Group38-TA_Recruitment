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
        <div class="badge">Sprint 1</div>
        <h1>Sign in to RecruitAssist</h1>
        <div class="hero-metrics section-gap">
            <div class="hero-metric"><strong>${totalUserCount}</strong><span>Total accounts</span></div>
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
                    <h2>Login</h2>
                </div>
                <span class="metric-pill">Password: ${demoPassword}</span>
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
                <button class="primary-button" type="submit" data-loading-text="Signing in...">Sign in</button>
            </form>
        </section>

        <section class="panel">
            <div class="section-head">
                <div>
                    <h2>Available accounts</h2>
                </div>
                <span class="metric-pill">Demo password</span>
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
                                <button class="secondary-button small-button" type="button" data-fill-login data-username="${demoUser.username}" data-password="${demoPassword}">
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
