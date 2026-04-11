<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Register · RecruitAssist</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css" />
    <script defer src="${pageContext.request.contextPath}/assets/js/app.js"></script>
</head>
<body>
<div class="page-shell narrow-shell">
    <header class="hero-card compact-hero">
        <div class="badge">New account</div>
        <h1>Create your RecruitAssist account</h1>
        <p class="subtitle">Register as a Teaching Assistant (TA) to browse and apply for positions, or as a Module Organiser (MO) to post and manage TA positions.</p>
    </header>

    <c:if test="${not empty flashMessage}">
        <div class="alert ${flashTone}">${flashMessage}</div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert error">${error}</div>
    </c:if>

    <main class="content-grid">
        <section class="panel">
            <div class="section-head">
                <div>
                    <h2>Registration form</h2>
                    <p class="muted-copy">Fill in the details below. You can complete your full profile after signing in.</p>
                </div>
            </div>
            <form class="form-grid" method="post" action="${pageContext.request.contextPath}/register">
                <label class="field-group">
                    <span>Username <em>(required)</em></span>
                    <input class="input" type="text" name="username" value="${username}"
                           placeholder="e.g. john.doe" required minlength="3" maxlength="30"
                           pattern="[a-zA-Z0-9._-]{3,30}"
                           title="3-30 characters: letters, numbers, dots, hyphens, underscores" />
                </label>
                <label class="field-group">
                    <span>Display name</span>
                    <input class="input" type="text" name="name" value="${name}"
                           placeholder="e.g. John Doe" maxlength="80" />
                </label>
                <label class="field-group">
                    <span>Email</span>
                    <input class="input" type="email" name="email" value="${email}"
                           placeholder="e.g. john@example.com" maxlength="120" />
                </label>
                <label class="field-group">
                    <span>Role</span>
                    <select class="input" name="role">
                        <option value="TA" ${selectedRole == 'TA' || empty selectedRole ? 'selected' : ''}>Teaching Assistant (TA)</option>
                        <option value="MO" ${selectedRole == 'MO' ? 'selected' : ''}>Module Organiser (MO)</option>
                    </select>
                </label>
                <label class="field-group">
                    <span>Password <em>(min 6 characters)</em></span>
                    <input class="input" type="password" name="password"
                           placeholder="Choose a strong password" required minlength="6" />
                </label>
                <label class="field-group">
                    <span>Confirm password</span>
                    <input class="input" type="password" name="confirmPassword"
                           placeholder="Re-enter your password" required minlength="6" />
                </label>
                <div class="surface-note">
                    <strong>Note:</strong> Admin accounts can only be created by system administrators. TA users can complete their full profile (skills, availability, CV) after signing in.
                </div>
                <button class="primary-button" type="submit">Create account</button>
                <a class="secondary-button" href="${pageContext.request.contextPath}/login">Already have an account? Sign in</a>
            </form>
        </section>
    </main>
</div>
</body>
</html>
