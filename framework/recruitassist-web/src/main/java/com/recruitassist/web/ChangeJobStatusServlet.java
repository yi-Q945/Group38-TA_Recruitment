package com.recruitassist.web;

import com.recruitassist.model.ActionResult;
import com.recruitassist.model.JobStatus;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.UserRole;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/jobs/status")
public class ChangeJobStatusServlet extends AppServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UserProfile user = requireAuthenticatedUser(req, resp);
        if (user == null || !requireRole(user, UserRole.MO, req, resp)) {
            return;
        }

        String jobId = req.getParameter("jobId");
        JobStatus targetStatus = JobStatus.from(req.getParameter("status")).orElse(null);
        if (targetStatus == null) {
            setFlash(req, "error", "Please choose a valid job status.");
            redirect(req, resp, resolveRedirectPath(req, jobId));
            return;
        }

        ActionResult result = services(req).jobService().changeJobStatus(user, jobId, targetStatus);
        setFlash(req, result.isSuccess() ? "success" : "error", result.getMessage());
        redirect(req, resp, resolveRedirectPath(req, jobId));
    }

    private String resolveRedirectPath(HttpServletRequest req, String jobId) {
        String returnTo = req.getParameter("returnTo");
        if ("detail".equalsIgnoreCase(returnTo) && jobId != null && !jobId.isBlank()) {
            StringBuilder path = new StringBuilder("/jobs/detail?jobId=")
                    .append(URLEncoder.encode(jobId, StandardCharsets.UTF_8));
            appendQueryParam(path, "sort", req.getParameter("sort"));
            appendQueryParam(path, "filterStatus", req.getParameter("filterStatus"));
            return path.toString();
        }
        return "/dashboard";
    }

    private void appendQueryParam(StringBuilder path, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        path.append('&')
                .append(key)
                .append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
