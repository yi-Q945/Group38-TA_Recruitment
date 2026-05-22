package com.recruitassist.web;

import com.recruitassist.config.AppPaths;
import com.recruitassist.model.JobPosting;
import com.recruitassist.model.UserProfile;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@WebServlet("/pdf/share")
public class SharedPdfServlet extends AppServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserProfile viewer = requireAuthenticatedUser(req, resp);
        if (viewer == null) {
            return;
        }

        String token = req.getParameter("token");
        String jobId = req.getParameter("jobId");
        UserProfile owner = services(req).pdfShareService().findOwnerByToken(token).orElse(null);
        if (owner == null || !owner.isCvAvailable()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Shared PDF link was not found.");
            return;
        }
        if (!services(req).pdfShareService().canAccess(viewer, owner, jobId)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "You are not allowed to access this shared PDF.");
            return;
        }

        Path cvDir = AppPaths.cvDir().toAbsolutePath().normalize();
        Path file = cvDir.resolve(owner.getCvFileName()).normalize();
        if (!file.startsWith(cvDir) || Files.notExists(file)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Shared PDF file was not found.");
            return;
        }

        String contentType = Files.probeContentType(file);
        resp.setContentType(contentType == null ? "application/octet-stream" : contentType);
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Content-Disposition", "inline; filename=\"" + friendlyFileName(owner, jobId, req) + "\"");
        Files.copy(file, resp.getOutputStream());
    }

    private String friendlyFileName(UserProfile owner, String jobId, HttpServletRequest req) {
        String extension = "";
        String cvName = owner.getCvFileName();
        int dotIndex = cvName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = cvName.substring(dotIndex);
        }

        String moduleCode = "";
        if (jobId != null && !jobId.isBlank()) {
            JobPosting job = services(req).jobService().findById(jobId.trim()).orElse(null);
            if (job != null) {
                moduleCode = job.getModuleCode().replaceAll("[^a-zA-Z0-9_-]", "_");
            }
        }
        String safeName = owner.getName().replaceAll("[^a-zA-Z0-9_ -]", "").replace(' ', '_');
        return moduleCode.isEmpty()
                ? safeName + "_CV" + extension
                : safeName + "_" + moduleCode + extension;
    }
}
