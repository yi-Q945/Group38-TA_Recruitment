package com.recruitassist.service;

import com.recruitassist.model.ApplicationRecord;
import com.recruitassist.model.ApplicationStatus;
import com.recruitassist.model.JobPosting;
import com.recruitassist.model.SystemConfig;
import com.recruitassist.model.UserRole;
import com.recruitassist.model.view.WorkloadEntry;
import com.recruitassist.repository.ApplicationRepository;
import com.recruitassist.repository.JobRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkloadService {
    private final UserService userService;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final SystemConfig systemConfig;

    public WorkloadService(
            UserService userService,
            JobRepository jobRepository,
            ApplicationRepository applicationRepository,
            SystemConfig systemConfig) {
        this.userService = userService;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.systemConfig = systemConfig;
    }

    public int getThreshold() {
        return systemConfig.getWorkload().getDefaultMaxHours();
    }

    public int workloadForUser(String userId) {
        return workloadByUserId().getOrDefault(userId, 0);
    }

    public long activeApplicationsForUser(String userId) {
        return applicationRepository.findByApplicantId(userId).stream()
                .filter(application -> application.getStatus() != ApplicationStatus.REJECTED)
                .filter(application -> application.getStatus() != ApplicationStatus.WITHDRAWN)
                .count();
    }

    public Map<String, Integer> workloadByUserId() {
        Map<String, JobPosting> jobsById = new LinkedHashMap<>();
        for (JobPosting job : jobRepository.findAll()) {
            jobsById.put(job.getJobId(), job);
        }

        Map<String, Integer> result = new LinkedHashMap<>();
        for (ApplicationRecord application : applicationRepository.findAll()) {
            if (application.getStatus() != ApplicationStatus.ACCEPTED) {
                continue;
            }
            JobPosting job = jobsById.get(application.getJobId());
            if (job == null) {
                continue;
            }
            result.merge(application.getApplicantId(), job.getWorkloadHours(), Integer::sum);
        }
        return result;
    }

    public List<WorkloadEntry> buildEntries() {
        Map<String, Integer> workload = workloadByUserId();
        int threshold = getThreshold();

        return userService.listUsersByRole(UserRole.TA).stream()
                .map(user -> new WorkloadEntry(
                        user,
                        workload.getOrDefault(user.getUserId(), 0),
                        activeApplicationsForUser(user.getUserId()),
                        threshold))
                .toList();
    }
}
