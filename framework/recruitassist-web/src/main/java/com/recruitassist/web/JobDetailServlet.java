package com.recruitassist.web;

import com.recruitassist.model.ApplicationRecord;
import com.recruitassist.model.ApplicationStatus;
import com.recruitassist.model.JobPosting;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.UserRole;
import com.recruitassist.model.view.JobRecommendation;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/jobs/detail")
public class JobDetailServlet extends AppServlet {
    private static final int AUTO_REFRESH_SECONDS = 30;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserProfile user = requireAuthenticatedUser(req, resp);
        if (user == null) {
            return;
        }

        moveFlashToRequest(req);
        String jobId = req.getParameter("jobId");
        if (jobId == null || jobId.isBlank()) {
            setFlash(req, "error", "Please choose a valid job.");
            redirect(req, resp, "/dashboard");
            return;
        }

        JobPosting job = services(req).jobService().findById(jobId).orElse(null);
        if (job == null) {
            setFlash(req, "error", "The requested job could not be found.");
            redirect(req, resp, "/dashboard");
            return;
        }

        List<ApplicationRecord> allApplications = services(req).applicationService().findByJobId(job.getJobId());
        long acceptedCount = allApplications.stream()
                .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                .count();

        UserProfile owner = services(req).userService().findById(job.getOwnerId()).orElse(null);
        boolean taView = user.getRole() == UserRole.TA;
        boolean adminView = user.getRole() == UserRole.ADMIN;
        boolean canReview = user.getRole() == UserRole.MO && job.getOwnerId().equalsIgnoreCase(user.getUserId());
        boolean canManage = canReview;
        boolean showApplications = canReview || adminView;

        req.setAttribute("user", user);
        req.setAttribute("appName", services(req).systemConfig().getAppName());
        req.setAttribute("job", job);
        req.setAttribute("ownerName", owner == null ? job.getOwnerId() : owner.getName());
        req.setAttribute("applicationCount", allApplications.size());
        req.setAttribute("acceptedCount", acceptedCount);
        req.setAttribute("remainingQuota", Math.max(job.getQuota() - acceptedCount, 0));
        req.setAttribute("taView", taView);
        req.setAttribute("adminView", adminView);
        req.setAttribute("canReview", canReview);
        req.setAttribute("canManage", canManage);
        req.setAttribute("showApplications", showApplications);
        req.setAttribute("workloadThreshold", services(req).workloadService().getThreshold());
        req.setAttribute("autoRefreshSeconds", AUTO_REFRESH_SECONDS);

        if (taView) {
            JobRecommendation recommendation = services(req).recommendationService().recommend(user, job);
            ApplicationRecord existingApplication = services(req).applicationService()
                    .findExistingApplication(user.getUserId(), job.getJobId())
                    .orElse(null);

            req.setAttribute("recommendation", recommendation);
            req.setAttribute("existingApplication", existingApplication);
            req.setAttribute("canApplyToJob", job.isAcceptingApplications((int) acceptedCount) && existingApplication == null);
            req.setAttribute(
                    "canWithdrawApplication",
                    existingApplication != null && existingApplication.getStatus() != ApplicationStatus.REJECTED);
            req.setAttribute("currentWorkload", services(req).workloadService().workloadForUser(user.getUserId()));
        }

        if (showApplications) {
            Map<String, Integer> workloadByUserId = services(req).workloadService().workloadByUserId();
            String candidateSort = normalizeCandidateSort(req.getParameter("sort"));
            String candidateStatusFilter = normalizeCandidateStatusFilter(req.getParameter("filterStatus"));
            List<ApplicationRecord> applications = services(req).applicationService().findByJobIdForReview(
                    job.getJobId(),
                    candidateSort,
                    candidateStatusFilter,
                    workloadByUserId);

            req.setAttribute("applications", applications);
            req.setAttribute("visibleApplicationCount", applications.size());
            req.setAttribute("usersById", services(req).userService().indexById());
            req.setAttribute("workloadByUserId", workloadByUserId);
            req.setAttribute("candidateSort", candidateSort);
            req.setAttribute("candidateSortLabel", describeCandidateSort(candidateSort));
            req.setAttribute("candidateStatusFilter", candidateStatusFilter);
            req.setAttribute("candidateStatusFilterLabel", describeCandidateStatusFilter(candidateStatusFilter));
        }

        req.getRequestDispatcher("/WEB-INF/jsp/job-detail.jsp").forward(req, resp);
    }

    private String normalizeCandidateSort(String rawSort) {
        if (rawSort == null || rawSort.isBlank()) {
            return "score";
        }
        return switch (rawSort.trim().toLowerCase()) {
            case "workload", "submitted", "status" -> rawSort.trim().toLowerCase();
            default -> "score";
        };
    }

    private String describeCandidateSort(String candidateSort) {
        return switch (candidateSort) {
            case "workload" -> "Lightest workload first";
            case "submitted" -> "Newest submission first";
            case "status" -> "Status priority";
            default -> "Best recommendation first";
        };
    }

    private String normalizeCandidateStatusFilter(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return "ALL";
        }
        return ApplicationStatus.from(rawStatus)
                .map(ApplicationStatus::getCode)
                .orElse("ALL");
    }

    private String describeCandidateStatusFilter(String candidateStatusFilter) {
        if ("ALL".equalsIgnoreCase(candidateStatusFilter)) {
            return "All statuses";
        }
        return ApplicationStatus.from(candidateStatusFilter)
                .map(ApplicationStatus::getLabel)
                .orElse("All statuses");
    }
}
