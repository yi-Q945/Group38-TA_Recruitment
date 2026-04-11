package com.recruitassist.web;

import com.recruitassist.config.AppPaths;
import com.recruitassist.model.ApplicationRecord;
import com.recruitassist.model.JobPosting;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.UserRole;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@WebServlet("/cv/download")
public class DownloadCvServlet extends AppServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserProfile viewer = requireAuthenticatedUser(req, resp);
        if (viewer == null) {
            return;
        }

        String userId = req.getParameter("userId");
        UserProfile owner = services(req).userService().findById(userId == null ? "" : userId.trim()).orElse(null);
        if (owner == null || !owner.isCvAvailable()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "CV file not found.");
            return;
        }
        if (!isAllowedToAccess(viewer, owner, req.getParameter("jobId"), req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "You are not allowed to access this CV.");
            return;
        }

        Path file = AppPaths.cvDir().resolve(owner.getCvFileName()).normalize();
        if (Files.notExists(file)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "CV file not found.");
            return;
        }

        String contentType = Files.probeContentType(file);
        resp.setContentType(contentType == null ? "application/octet-stream" : contentType);
        resp.setHeader("X-Content-Type-Options", "nosniff");

        // Build friendly filename: Name_ModuleCode.ext
        String extension = "";
        String cvName = owner.getCvFileName();
        int dotIndex = cvName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = cvName.substring(dotIndex);
        }
        String jobId = req.getParameter("jobId");
        String moduleCode = "";
        if (jobId != null && !jobId.isBlank()) {
            JobPosting job = services(req).jobService().findById(jobId.trim()).orElse(null);
            if (job != null) {
                moduleCode = job.getModuleCode().replaceAll("[^a-zA-Z0-9_-]", "_");
            }
        }
        String safeName = owner.getName().replaceAll("[^a-zA-Z0-9_ -]", "").replace(' ', '_');
        String downloadName = moduleCode.isEmpty()
                ? safeName + "_CV" + extension
                : safeName + "_" + moduleCode + extension;

        resp.setHeader("Content-Disposition", "attachment; filename=\"" + downloadName + "\"");
        Files.copy(file, resp.getOutputStream());
    }

    private boolean isAllowedToAccess(UserProfile viewer, UserProfile owner, String jobId, HttpServletRequest req) {
        if (viewer.getRole() == UserRole.ADMIN) {
            return true;
        }
        if (viewer.getUserId().equalsIgnoreCase(owner.getUserId())) {
            return true;
        }
        if (viewer.getRole() != UserRole.MO || jobId == null || jobId.isBlank()) {
            return false;
        }

        JobPosting job = services(req).jobService().findById(jobId.trim()).orElse(null);
        if (job == null || !job.getOwnerId().equalsIgnoreCase(viewer.getUserId())) {
            return false;
        }
        for (ApplicationRecord application : services(req).applicationService().findByJobId(job.getJobId())) {
            if (application.getApplicantId().equalsIgnoreCase(owner.getUserId())) {
                return true;
            }
        }
        return false;
    }
}
