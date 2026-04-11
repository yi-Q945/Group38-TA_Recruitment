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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/dashboard")
public class DashboardServlet extends AppServlet {
    private static final int AUTO_REFRESH_SECONDS = 60;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserProfile user = requireAuthenticatedUser(req, resp);
        if (user == null) {
            return;
        }

        moveFlashToRequest(req);
        req.setAttribute("user", user);
        req.setAttribute("appName", services(req).systemConfig().getAppName());

        if (user.getRole() == UserRole.TA) {
            req.setAttribute("autoRefreshSeconds", 0);
            renderTaDashboard(req, resp, user);
            return;
        }

        req.setAttribute("autoRefreshSeconds", AUTO_REFRESH_SECONDS);

        if (user.getRole() == UserRole.MO) {
            renderMoDashboard(req, resp, user);
            return;
        }
        renderAdminDashboard(req, resp, user);
    }

    private void renderTaDashboard(HttpServletRequest req, HttpServletResponse resp, UserProfile user)
            throws ServletException, IOException {
        String jobSearchQuery = normalizeText(req.getParameter("q"));
        String jobSort = normalizeTaSort(req.getParameter("sort"));

        List<JobRecommendation> recommendedJobs = services(req).recommendationService().recommendJobsFor(user).stream()
                .filter(recommendation -> matchesRecommendationQuery(recommendation, jobSearchQuery))
                .sorted(taComparator(jobSort))
                .toList();
        List<ApplicationRecord> applications = services(req).applicationService().findByApplicantId(user.getUserId());
        long acceptedApplications = applications.stream()
                .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                .count();

        req.setAttribute("recommendedJobs", recommendedJobs);
        req.setAttribute("topRecommendation", recommendedJobs.isEmpty() ? null : recommendedJobs.get(0));
        req.setAttribute("applications", applications);
        req.setAttribute("applicationsByJobId", services(req).applicationService().mapByJobIdForApplicant(user.getUserId()));
        req.setAttribute("jobsById", services(req).jobService().indexById());
        req.setAttribute("currentWorkload", services(req).workloadService().workloadForUser(user.getUserId()));
        req.setAttribute("activeApplicationCount", services(req).workloadService().activeApplicationsForUser(user.getUserId()));
        req.setAttribute("acceptedApplicationCount", acceptedApplications);
        req.setAttribute("profileSignalCount", user.getProfileSignalCount());
        req.setAttribute("profileSignalPercent", user.getProfileSignalPercent());
        req.setAttribute("profileReady", user.isProfileReady());
        req.setAttribute("workloadThreshold", services(req).workloadService().getThreshold());
        req.setAttribute("jobSearchQuery", req.getParameter("q") == null ? "" : req.getParameter("q").trim());
        req.setAttribute("jobSort", jobSort);
        req.setAttribute("jobSortLabel", describeTaSort(jobSort));
        req.getRequestDispatcher("/WEB-INF/jsp/dashboard-ta.jsp").forward(req, resp);
    }

    private void renderMoDashboard(HttpServletRequest req, HttpServletResponse resp, UserProfile user)
            throws ServletException, IOException {
        List<JobPosting> jobs = services(req).jobService().listJobsForOwner(user.getUserId());
        Map<String, Integer> workloadByUserId = services(req).workloadService().workloadByUserId();
        Map<String, List<ApplicationRecord>> applicationsByJobId = services(req).applicationService().groupByJobIdsForReview(
                jobs.stream().map(JobPosting::getJobId).toList(),
                workloadByUserId);
        long openJobCount = jobs.stream().filter(JobPosting::isOpen).count();
        int totalApplicationCount = applicationsByJobId.values().stream().mapToInt(List::size).sum();
        long acceptedCandidateCount = applicationsByJobId.values().stream()
                .flatMap(List::stream)
                .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                .count();
        long shortlistedCount = applicationsByJobId.values().stream()
                .flatMap(List::stream)
                .filter(application -> application.getStatus() == ApplicationStatus.SHORTLISTED)
                .count();

        req.setAttribute("jobs", jobs);
        req.setAttribute("applicationsByJobId", applicationsByJobId);
        req.setAttribute("usersById", services(req).userService().indexById());
        req.setAttribute("workloadByUserId", workloadByUserId);
        req.setAttribute("workloadThreshold", services(req).workloadService().getThreshold());
        req.setAttribute("openJobCount", openJobCount);
        req.setAttribute("totalApplicationCount", totalApplicationCount);
        req.setAttribute("acceptedCandidateCount", acceptedCandidateCount);
        req.setAttribute("shortlistedCandidateCount", shortlistedCount);
        req.getRequestDispatcher("/WEB-INF/jsp/dashboard-mo.jsp").forward(req, resp);
    }

    private void renderAdminDashboard(HttpServletRequest req, HttpServletResponse resp, UserProfile user)
            throws ServletException, IOException {
        String statusFilter = normalizeAdminStatus(req.getParameter("jobStatus"));
        String moduleQuery = normalizeText(req.getParameter("module"));

        List<JobPosting> allJobs = services(req).jobService().listAllJobs();
        List<JobPosting> visibleJobs = allJobs.stream()
                .filter(job -> matchesAdminFilter(job, statusFilter, moduleQuery))
                .toList();
        Map<String, List<ApplicationRecord>> applicationsByJobId = services(req).applicationService().groupByJobIds(
                visibleJobs.stream().map(JobPosting::getJobId).toList());
        Map<String, Long> acceptedByJobId = new LinkedHashMap<>();
        for (JobPosting job : visibleJobs) {
            long accepted = applicationsByJobId.getOrDefault(job.getJobId(), List.of()).stream()
                    .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                    .count();
            acceptedByJobId.put(job.getJobId(), accepted);
        }

        req.setAttribute("user", user);
        req.setAttribute("workloadEntries", services(req).workloadService().buildEntries());
        req.setAttribute("latestApplications", services(req).applicationService().findRecentApplications(10));
        req.setAttribute("usersById", services(req).userService().indexById());
        req.setAttribute("jobsById", services(req).jobService().indexById());
        req.setAttribute("workloadThreshold", services(req).workloadService().getThreshold());
        req.setAttribute("adminJobs", visibleJobs);
        req.setAttribute("applicationsByJobId", applicationsByJobId);
        req.setAttribute("acceptedByJobId", acceptedByJobId);
        req.setAttribute("jobStatusFilter", statusFilter);
        req.setAttribute("jobStatusFilterLabel", describeAdminStatus(statusFilter));
        req.setAttribute("moduleQuery", req.getParameter("module") == null ? "" : req.getParameter("module").trim());
        req.setAttribute("openJobTotal", allJobs.stream().filter(JobPosting::isOpen).count());
        req.setAttribute("jobCount", allJobs.size());
        req.setAttribute("visibleJobCount", visibleJobs.size());
        req.setAttribute("visibleApplicantCount", applicationsByJobId.values().stream().mapToInt(List::size).sum());
        req.getRequestDispatcher("/WEB-INF/jsp/dashboard-admin.jsp").forward(req, resp);
    }

    private Comparator<JobRecommendation> taComparator(String jobSort) {
        return switch (jobSort) {
            case "deadline" -> Comparator.comparing((JobRecommendation recommendation) -> recommendation.getJob().getDeadline())
                    .thenComparing(Comparator.comparingDouble(JobRecommendation::getScore).reversed());
            case "workload" -> Comparator.comparingInt(JobRecommendation::getProjectedWorkloadHours)
                    .thenComparing(Comparator.comparingDouble(JobRecommendation::getScore).reversed());
            default -> Comparator.comparingDouble(JobRecommendation::getScore).reversed()
                    .thenComparing(recommendation -> recommendation.getJob().getDeadline());
        };
    }

    private boolean matchesRecommendationQuery(JobRecommendation recommendation, String query) {
        if (query.isBlank()) {
            return true;
        }
        String searchable = String.join(" ",
                recommendation.getJob().getTitle(),
                recommendation.getJob().getModuleCode(),
                recommendation.getJob().getDescription(),
                recommendation.getJob().getRequiredSkillsSummary(),
                recommendation.getJob().getPreferredSkillsSummary(),
                recommendation.getMatchedSkillsSummary(),
                recommendation.getMissingSkillsSummary()).toLowerCase();
        return searchable.contains(query);
    }

    private boolean matchesAdminFilter(JobPosting job, String statusFilter, String moduleQuery) {
        if (!"ALL".equals(statusFilter) && !job.getStatus().getCode().equalsIgnoreCase(statusFilter)) {
            return false;
        }
        if (moduleQuery.isBlank()) {
            return true;
        }
        String searchable = (job.getModuleCode() + " " + job.getTitle() + " " + job.getDescription()).toLowerCase();
        return searchable.contains(moduleQuery);
    }

    private String normalizeTaSort(String rawSort) {
        if (rawSort == null || rawSort.isBlank()) {
            return "score";
        }
        return switch (rawSort.trim().toLowerCase()) {
            case "deadline", "workload" -> rawSort.trim().toLowerCase();
            default -> "score";
        };
    }

    private String describeTaSort(String jobSort) {
        return switch (jobSort) {
            case "deadline" -> "Closest deadline first";
            case "workload" -> "Lowest projected workload first";
            default -> "Best match first";
        };
    }

    private String normalizeAdminStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return "ALL";
        }
        return switch (rawStatus.trim().toUpperCase()) {
            case "OPEN", "CLOSED" -> rawStatus.trim().toUpperCase();
            default -> "ALL";
        };
    }

    private String describeAdminStatus(String status) {
        return switch (status) {
            case "OPEN" -> "Open jobs only";
            case "CLOSED" -> "Closed jobs only";
            default -> "All jobs";
        };
    }

    private String normalizeText(String rawValue) {
        return rawValue == null ? "" : rawValue.trim().toLowerCase();
    }
}
