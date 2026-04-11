package com.recruitassist.web;

import com.recruitassist.model.ActionResult;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.UserRole;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/applications/withdraw")
public class WithdrawApplicationServlet extends AppServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UserProfile user = requireAuthenticatedUser(req, resp);
        if (user == null || !requireRole(user, UserRole.TA, req, resp)) {
            return;
        }

        String applicationId = req.getParameter("applicationId");
        String jobId = req.getParameter("jobId");
        ActionResult result = services(req).applicationService().withdrawApplication(applicationId, user);
        setFlash(req, result.isSuccess() ? "success" : "error", result.getMessage());
        redirect(req, resp, resolveRedirectPath(req, jobId));
    }

    private String resolveRedirectPath(HttpServletRequest req, String jobId) {
        String returnTo = req.getParameter("returnTo");
        if ("detail".equalsIgnoreCase(returnTo) && jobId != null && !jobId.isBlank()) {
            return "/jobs/detail?jobId=" + URLEncoder.encode(jobId, StandardCharsets.UTF_8);
        }
        return "/dashboard";
    }
}
