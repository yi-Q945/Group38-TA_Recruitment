package com.recruitassist.service;

import com.recruitassist.model.ActionResult;
import com.recruitassist.model.ApplicationRecord;
import com.recruitassist.model.ApplicationStatus;
import com.recruitassist.model.JobPosting;
import com.recruitassist.model.JobStatus;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.UserRole;
import com.recruitassist.model.view.JobRecommendation;
import com.recruitassist.repository.ApplicationRepository;
import com.recruitassist.repository.AuditRepository;
import com.recruitassist.repository.IdCounterRepository;
import com.recruitassist.repository.JobRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final IdCounterRepository idCounterRepository;
    private final AuditRepository auditRepository;
    private final RecommendationService recommendationService;
    private final UserService userService;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ApplicationService(
            ApplicationRepository applicationRepository,
            JobRepository jobRepository,
            IdCounterRepository idCounterRepository,
            AuditRepository auditRepository,
            RecommendationService recommendationService,
            UserService userService) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.idCounterRepository = idCounterRepository;
        this.auditRepository = auditRepository;
        this.recommendationService = recommendationService;
        this.userService = userService;
    }

    public List<ApplicationRecord> findAll() {
        lock.readLock().lock();
        try {
            return refreshRecommendationSnapshots(applicationRepository.findAll());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<ApplicationRecord> findByApplicantId(String applicantId) {
        lock.readLock().lock();
        try {
            List<ApplicationRecord> applications = applicationRepository.findByApplicantId(applicantId);
            return refreshRecommendationSnapshots(applications);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<ApplicationRecord> findByJobId(String jobId) {
        lock.readLock().lock();
        try {
            List<ApplicationRecord> applications = applicationRepository.findByJobId(jobId);
            return refreshRecommendationSnapshots(applications);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<ApplicationRecord> findByJobIdForReview(
            String jobId,
            String sortBy,
            String filterStatus,
            Map<String, Integer> workloadByUserId) {
        lock.readLock().lock();
        try {
            ApplicationStatus statusFilter = parseStatusFilter(filterStatus);
            Map<String, Integer> safeWorkloadByUserId = workloadByUserId == null ? Map.of() : workloadByUserId;
            List<ApplicationRecord> applications = refreshRecommendationSnapshots(applicationRepository.findByJobId(jobId)).stream()
                    .filter(application -> statusFilter == null || application.getStatus() == statusFilter)
                    .toList();
            return sortForReview(applications, normalizeReviewSort(sortBy), safeWorkloadByUserId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<ApplicationRecord> findExistingApplication(String applicantId, String jobId) {
        lock.readLock().lock();
        try {
            return refreshRecommendationSnapshots(applicationRepository.findByApplicantId(applicantId)).stream()
                    .filter(application -> application.getJobId().equalsIgnoreCase(jobId))
                    .filter(application -> application.getStatus() != ApplicationStatus.WITHDRAWN)
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<String, ApplicationRecord> mapByJobIdForApplicant(String applicantId) {
        Map<String, ApplicationRecord> result = new LinkedHashMap<>();
        for (ApplicationRecord application : findByApplicantId(applicantId)) {
            if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
                continue;
            }
            result.putIfAbsent(application.getJobId(), application);
        }
        return result;
    }

    public Map<String, List<ApplicationRecord>> groupByJobIds(Collection<String> jobIds) {
        return groupByJobIdsForReview(jobIds, Map.of());
    }

    public Map<String, List<ApplicationRecord>> groupByJobIdsForReview(
            Collection<String> jobIds,
            Map<String, Integer> workloadByUserId) {
        lock.readLock().lock();
        try {
            Set<String> requestedIds = jobIds == null ? Set.of() : jobIds.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (requestedIds.isEmpty()) {
                return Map.of();
            }

            List<ApplicationRecord> refreshed = refreshRecommendationSnapshots(applicationRepository.findAll());
            Map<String, Integer> safeWorkloadByUserId = workloadByUserId == null ? Map.of() : workloadByUserId;
            Map<String, List<ApplicationRecord>> grouped = new LinkedHashMap<>();
            for (String jobId : requestedIds) {
                List<ApplicationRecord> applications = refreshed.stream()
                        .filter(application -> application.getJobId().equalsIgnoreCase(jobId))
                        .toList();
                grouped.put(jobId, sortForReview(applications, "score", safeWorkloadByUserId));
            }
            return grouped;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<ApplicationRecord> findRecentApplications(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return findAll().stream().limit(limit).toList();
    }

    public ActionResult submitApplication(UserProfile user, String jobId) {
        lock.writeLock().lock();
        try {
            if (user == null || user.getRole() != UserRole.TA) {
                return ActionResult.failure("Only teaching assistants can apply for jobs.");
            }
            if (!user.isProfileReady()) {
                return ActionResult.failure("Please complete your profile with programme, skills and availability before applying.");
            }

            JobPosting job = jobRepository.findById(jobId).orElse(null);
            if (job == null) {
                return ActionResult.failure("The selected job does not exist.");
            }
            if (job.isExpired()) {
                closeJobIfNeeded(job);
                return ActionResult.failure("This job has passed its deadline and is no longer accepting applications.");
            }
            if (!job.isOpen()) {
                return ActionResult.failure("This job is already closed.");
            }
            if (countAcceptedApplications(jobId) >= job.getQuota()) {
                closeJobIfNeeded(job);
                return ActionResult.failure("This job has already reached its quota.");
            }

            boolean duplicate = applicationRepository.findByApplicantId(user.getUserId()).stream()
                    .anyMatch(application -> application.getJobId().equalsIgnoreCase(jobId)
                            && application.getStatus() != ApplicationStatus.WITHDRAWN);
            if (duplicate) {
                return ActionResult.failure("You have already applied for this job.");
            }

            JobRecommendation recommendation = recommendationService.recommend(user, job);
            ApplicationRecord application = new ApplicationRecord();
            application.setApplicationId(idCounterRepository.nextApplicationId());
            application.setJobId(jobId);
            application.setApplicantId(user.getUserId());
            application.setApplyTime(Instant.now().toString());
            application.setStatus(ApplicationStatus.SUBMITTED);
            application.setRecommendationScore(recommendation.getScore());
            application.setExplanation(recommendation.getReasons());
            applicationRepository.save(application);
            auditRepository.append(user.getUserId(), "APPLY", jobId, "SUCCESS", "score=" + application.getRecommendationPercent());
            return ActionResult.success("Application submitted successfully.");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ActionResult withdrawApplication(String applicationId, UserProfile actor) {
        if (actor == null || actor.getRole() != UserRole.TA) {
            return ActionResult.failure("Only teaching assistants can withdraw applications.");
        }

        lock.writeLock().lock();
        try {
            String cleanApplicationId = clean(applicationId);
            ApplicationRecord application = applicationRepository.findById(cleanApplicationId).orElse(null);
            if (application == null) {
                return ActionResult.failure("Application not found.");
            }
            if (!Objects.equals(application.getApplicantId(), actor.getUserId())) {
                return ActionResult.failure("You can only withdraw your own applications.");
            }
            if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
                return ActionResult.failure("This application has already been withdrawn.");
            }
            if (application.getStatus() == ApplicationStatus.REJECTED) {
                return ActionResult.failure("Rejected applications do not need to be withdrawn.");
            }

            application.setStatus(ApplicationStatus.WITHDRAWN);
            applicationRepository.save(application);
            auditRepository.append(actor.getUserId(), "WITHDRAW_APPLICATION", cleanApplicationId, "SUCCESS", application.getJobId());
            return ActionResult.success("Application withdrawn successfully.");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ActionResult updateStatus(String applicationId, ApplicationStatus newStatus, UserProfile actor) {
        lock.writeLock().lock();
        try {
            if (newStatus == ApplicationStatus.WITHDRAWN) {
                return ActionResult.failure("Withdrawn status can only be set by the applicant.");
            }

            ApplicationRecord application = applicationRepository.findById(applicationId).orElse(null);
            if (application == null) {
                return ActionResult.failure("Application not found.");
            }
            if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
                return ActionResult.failure("Withdrawn applications can no longer be reviewed.");
            }

            JobPosting job = jobRepository.findById(application.getJobId()).orElse(null);
            if (job == null) {
                return ActionResult.failure("Related job not found.");
            }
            if (!Objects.equals(job.getOwnerId(), actor.getUserId())) {
                return ActionResult.failure("You can only update applications for your own jobs.");
            }

            if (newStatus == ApplicationStatus.ACCEPTED) {
                long acceptedCount = countAcceptedApplications(job.getJobId());
                if (application.getStatus() != ApplicationStatus.ACCEPTED && acceptedCount >= job.getQuota()) {
                    closeJobIfNeeded(job);
                    return ActionResult.failure("Quota has already been reached for this job.");
                }
            }

            application.setStatus(newStatus);
            applicationRepository.save(application);
            auditRepository.append(actor.getUserId(), "UPDATE_STATUS", applicationId, "SUCCESS", newStatus.getCode());

            String message = "Application updated to " + newStatus.getLabel() + '.';
            if (newStatus == ApplicationStatus.ACCEPTED && countAcceptedApplications(job.getJobId()) >= job.getQuota()) {
                closeJobIfNeeded(job);
                message += " The job was automatically closed because the quota is now full.";
            }
            return ActionResult.success(message);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<ApplicationRecord> sortForReview(
            List<ApplicationRecord> applications,
            String normalizedSort,
            Map<String, Integer> workloadByUserId) {
        Comparator<ApplicationRecord> recommendationComparator =
                Comparator.comparingDouble(ApplicationRecord::getRecommendationScore).reversed();
        Comparator<ApplicationRecord> submittedComparator =
                Comparator.comparing(ApplicationRecord::getApplyTime, String.CASE_INSENSITIVE_ORDER).reversed();
        Comparator<ApplicationRecord> workloadComparator =
                Comparator.comparingInt(application -> workloadByUserId.getOrDefault(application.getApplicantId(), 0));
        Comparator<ApplicationRecord> statusComparator =
                Comparator.comparingInt(application -> statusRank(application.getStatus()));

        Comparator<ApplicationRecord> comparator = switch (normalizedSort) {
            case "workload" -> workloadComparator
                    .thenComparing(recommendationComparator)
                    .thenComparing(submittedComparator);
            case "submitted" -> submittedComparator
                    .thenComparing(recommendationComparator)
                    .thenComparing(statusComparator);
            case "status" -> statusComparator
                    .thenComparing(recommendationComparator)
                    .thenComparing(workloadComparator)
                    .thenComparing(submittedComparator);
            default -> recommendationComparator
                    .thenComparing(statusComparator)
                    .thenComparing(workloadComparator)
                    .thenComparing(submittedComparator);
        };

        return applications.stream()
                .sorted(comparator)
                .toList();
    }

    private String normalizeReviewSort(String rawSort) {
        if (rawSort == null || rawSort.isBlank()) {
            return "score";
        }
        return switch (rawSort.trim().toLowerCase()) {
            case "workload", "submitted", "status" -> rawSort.trim().toLowerCase();
            default -> "score";
        };
    }

    private ApplicationStatus parseStatusFilter(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank() || "ALL".equalsIgnoreCase(rawStatus.trim())) {
            return null;
        }
        return ApplicationStatus.from(rawStatus).orElse(null);
    }

    private int statusRank(ApplicationStatus status) {
        return switch (status) {
            case ACCEPTED -> 0;
            case SHORTLISTED -> 1;
            case SUBMITTED -> 2;
            case REJECTED -> 3;
            case WITHDRAWN -> 4;
        };
    }

    private List<ApplicationRecord> refreshRecommendationSnapshots(List<ApplicationRecord> source) {
        if (source.isEmpty()) {
            return List.of();
        }
        Map<String, UserProfile> usersById = userService.indexById();
        Map<String, JobPosting> jobsById = jobRepository.findAll().stream()
                .collect(Collectors.toMap(
                        JobPosting::getJobId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
        return source.stream()
                .map(application -> refreshRecommendationSnapshot(application, usersById, jobsById))
                .toList();
    }

    private ApplicationRecord refreshRecommendationSnapshot(
            ApplicationRecord source,
            Map<String, UserProfile> usersById,
            Map<String, JobPosting> jobsById) {
        ApplicationRecord copy = cloneApplication(source);
        UserProfile applicant = usersById.get(source.getApplicantId());
        JobPosting job = jobsById.get(source.getJobId());
        if (applicant == null || job == null) {
            return copy;
        }

        JobRecommendation recommendation = recommendationService.recommend(applicant, job);
        copy.setRecommendationScore(recommendation.getScore());
        copy.setExplanation(recommendation.getReasons());
        return copy;
    }

    private ApplicationRecord cloneApplication(ApplicationRecord source) {
        ApplicationRecord copy = new ApplicationRecord();
        copy.setApplicationId(source.getApplicationId());
        copy.setJobId(source.getJobId());
        copy.setApplicantId(source.getApplicantId());
        copy.setApplyTime(source.getApplyTime());
        copy.setStatus(source.getStatus());
        copy.setRecommendationScore(source.getRecommendationScore());
        copy.setExplanation(List.copyOf(source.getExplanation()));
        return copy;
    }

    private long countAcceptedApplications(String jobId) {
        return applicationRepository.findByJobId(jobId).stream()
                .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                .count();
    }

    private void closeJobIfNeeded(JobPosting job) {
        if (job != null && job.isOpen()) {
            job.setStatus(JobStatus.CLOSED);
            jobRepository.save(job);
        }
    }

    private String clean(String rawValue) {
        return rawValue == null ? "" : rawValue.trim();
    }
}
