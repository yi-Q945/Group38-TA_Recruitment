package com.recruitassist.web;

import com.recruitassist.model.ApplicationRecord;
import com.recruitassist.model.JobPosting;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.UserRole;
import com.recruitassist.model.view.WorkloadEntry;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/admin/export")
public class AdminExportServlet extends AppServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UserProfile user = requireAuthenticatedUser(req, resp);
        if (user == null || !requireRole(user, UserRole.ADMIN, req, resp)) {
            return;
        }

        String type = normalizeType(req.getParameter("type"));
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"recruitassist-"
                + type + '-' + LocalDate.now() + ".csv\"");

        String csv = switch (type) {
            case "applications" -> applicationsCsv(req);
            case "workload" -> workloadCsv(req);
            default -> jobsCsv(req);
        };
        resp.getWriter().write(csv);
    }

    private String jobsCsv(HttpServletRequest req) {
        List<JobPosting> jobs = services(req).jobService().listAllJobs();
        Map<String, UserProfile> usersById = services(req).userService().indexById();
        Map<String, List<ApplicationRecord>> applicationsByJobId = services(req).applicationService()
                .groupByJobIds(jobs.stream().map(JobPosting::getJobId).toList());
        StringBuilder csv = new StringBuilder("jobId,moduleCode,title,owner,deadline,status,quota,workloadHours,applications,accepted\n");
        for (JobPosting job : jobs) {
            List<ApplicationRecord> applications = applicationsByJobId.getOrDefault(job.getJobId(), List.of());
            long accepted = applications.stream().filter(app -> app.getStatus().getCode().equals("ACCEPTED")).count();
            appendRow(csv,
                    job.getJobId(),
                    job.getModuleCode(),
                    job.getTitle(),
                    usersById.getOrDefault(job.getOwnerId(), new UserProfile()).getName(),
                    job.getDeadlineLabel(),
                    job.getStatus().getLabel(),
                    String.valueOf(job.getQuota()),
                    String.valueOf(job.getWorkloadHours()),
                    String.valueOf(applications.size()),
                    String.valueOf(accepted));
        }
        return csv.toString();
    }

    private String applicationsCsv(HttpServletRequest req) {
        Map<String, UserProfile> usersById = services(req).userService().indexById();
        Map<String, JobPosting> jobsById = services(req).jobService().indexById();
        StringBuilder csv = new StringBuilder("applicationId,jobId,moduleCode,jobTitle,applicant,status,recommendationPercent,submittedAt\n");
        for (ApplicationRecord application : services(req).applicationService().findAll()) {
            JobPosting job = jobsById.get(application.getJobId());
            UserProfile applicant = usersById.get(application.getApplicantId());
            appendRow(csv,
                    application.getApplicationId(),
                    application.getJobId(),
                    job == null ? "" : job.getModuleCode(),
                    job == null ? "" : job.getTitle(),
                    applicant == null ? application.getApplicantId() : applicant.getName(),
                    application.getStatus().getLabel(),
                    String.valueOf(application.getRecommendationPercent()),
                    application.getApplyTimeLabel());
        }
        return csv.toString();
    }

    private String workloadCsv(HttpServletRequest req) {
        StringBuilder csv = new StringBuilder("userId,name,programme,acceptedHours,activeApplications,threshold,status\n");
        for (WorkloadEntry entry : services(req).workloadService().buildEntries()) {
            appendRow(csv,
                    entry.getUser().getUserId(),
                    entry.getUser().getName(),
                    entry.getUser().getProgramme(),
                    String.valueOf(entry.getAcceptedHours()),
                    String.valueOf(entry.getActiveApplications()),
                    String.valueOf(entry.getThreshold()),
                    entry.isOverloaded() ? "Over threshold" : "Balanced");
        }
        return csv.toString();
    }

    private String normalizeType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return "jobs";
        }
        return switch (rawType.trim().toLowerCase()) {
            case "applications", "workload" -> rawType.trim().toLowerCase();
            default -> "jobs";
        };
    }

    private void appendRow(StringBuilder csv, String... values) {
        csv.append(java.util.Arrays.stream(values)
                .map(this::csvField)
                .collect(Collectors.joining(",")))
                .append('\n');
    }

    private String csvField(String value) {
        String safeValue = value == null ? "" : value.replace("\"", "\"\"");
        return '"' + safeValue + '"';
    }
}
