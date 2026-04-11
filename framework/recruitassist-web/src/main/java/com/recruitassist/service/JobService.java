package com.recruitassist.service;

import com.recruitassist.model.ActionResult;
import com.recruitassist.model.ApplicationRecord;
import com.recruitassist.model.ApplicationStatus;
import com.recruitassist.model.JobPosting;
import com.recruitassist.model.JobStatus;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.UserRole;
import com.recruitassist.repository.ApplicationRepository;
import com.recruitassist.repository.AuditRepository;
import com.recruitassist.repository.IdCounterRepository;
import com.recruitassist.repository.JobRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JobService {
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final IdCounterRepository idCounterRepository;
    private final AuditRepository auditRepository;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public JobService(
            JobRepository jobRepository,
            ApplicationRepository applicationRepository,
            IdCounterRepository idCounterRepository,
            AuditRepository auditRepository) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.idCounterRepository = idCounterRepository;
        this.auditRepository = auditRepository;
    }

    public List<JobPosting> listAllJobs() {
        lock.writeLock().lock();
        try {
            return synchronizeOperationalStates(jobRepository.findAll());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<JobPosting> listOpenJobs() {
        return listAllJobs().stream()
                .filter(JobPosting::isOpen)
                .toList();
    }

    public List<JobPosting> listJobsForOwner(String ownerId) {
        return listAllJobs().stream()
                .filter(job -> job.getOwnerId().equalsIgnoreCase(ownerId))
                .toList();
    }

    public Optional<JobPosting> findById(String jobId) {
        return listAllJobs().stream()
                .filter(job -> job.getJobId().equalsIgnoreCase(jobId))
                .findFirst();
    }

    public Map<String, JobPosting> indexById() {
        return listAllJobs().stream()
                .collect(Collectors.toMap(
                        JobPosting::getJobId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    public ActionResult createJob(
            UserProfile actor,
            String title,
            String moduleCode,
            String description,
            String requiredSkillsRaw,
            String preferredSkillsRaw,
            String deadline,
            String quotaRaw,
            String workloadHoursRaw) {
        if (actor == null || actor.getRole() != UserRole.MO) {
            return ActionResult.failure("Only module organisers can create jobs.");
        }

        lock.writeLock().lock();
        try {
            ValidatedJobInput input;
            try {
                input = validateJobInput(
                        title,
                        moduleCode,
                        description,
                        requiredSkillsRaw,
                        preferredSkillsRaw,
                        deadline,
                        quotaRaw,
                        workloadHoursRaw);
            } catch (IllegalArgumentException ex) {
                return ActionResult.failure(ex.getMessage());
            }

            JobPosting jobPosting = new JobPosting();
            jobPosting.setJobId(idCounterRepository.nextJobId());
            jobPosting.setOwnerId(actor.getUserId());
            jobPosting.setStatus(JobStatus.OPEN);
            applyJobInput(jobPosting, input);

            jobRepository.save(jobPosting);
            auditRepository.append(
                    actor.getUserId(),
                    "CREATE_JOB",
                    jobPosting.getJobId(),
                    "SUCCESS",
                    input.moduleCode() + " · " + input.title());
            return ActionResult.success("Job " + jobPosting.getJobId() + " created successfully.");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ActionResult updateJob(
            UserProfile actor,
            String jobId,
            String title,
            String moduleCode,
            String description,
            String requiredSkillsRaw,
            String preferredSkillsRaw,
            String deadline,
            String quotaRaw,
            String workloadHoursRaw) {
        if (actor == null || actor.getRole() != UserRole.MO) {
            return ActionResult.failure("Only module organisers can update jobs.");
        }

        lock.writeLock().lock();
        try {
            String cleanJobId = clean(jobId);
            if (cleanJobId.isBlank()) {
                return ActionResult.failure("A valid job id is required.");
            }

            JobPosting existingJob = jobRepository.findById(cleanJobId).orElse(null);
            if (existingJob == null) {
                return ActionResult.failure("The selected job could not be found.");
            }
            if (!Objects.equals(existingJob.getOwnerId(), actor.getUserId())) {
                return ActionResult.failure("You can only edit jobs that you own.");
            }

            ValidatedJobInput input;
            try {
                input = validateJobInput(
                        title,
                        moduleCode,
                        description,
                        requiredSkillsRaw,
                        preferredSkillsRaw,
                        deadline,
                        quotaRaw,
                        workloadHoursRaw);
            } catch (IllegalArgumentException ex) {
                return ActionResult.failure(ex.getMessage());
            }

            long acceptedCount = countAcceptedApplications(cleanJobId);
            if (input.quota() < acceptedCount) {
                return ActionResult.failure("Quota cannot be lower than the current accepted count of " + acceptedCount + '.');
            }

            applyJobInput(existingJob, input);
            if (existingJob.isExpired() || acceptedCount >= existingJob.getQuota()) {
                existingJob.setStatus(JobStatus.CLOSED);
            }
            jobRepository.save(existingJob);
            auditRepository.append(
                    actor.getUserId(),
                    "UPDATE_JOB",
                    existingJob.getJobId(),
                    "SUCCESS",
                    input.moduleCode() + " · " + input.title());
            return ActionResult.success("Job " + existingJob.getJobId() + " updated successfully.");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ActionResult changeJobStatus(UserProfile actor, String jobId, JobStatus targetStatus) {
        if (actor == null || actor.getRole() != UserRole.MO) {
            return ActionResult.failure("Only module organisers can change job status.");
        }
        if (targetStatus == null) {
            return ActionResult.failure("Please choose a valid job status.");
        }

        lock.writeLock().lock();
        try {
            String cleanJobId = clean(jobId);
            JobPosting jobPosting = jobRepository.findById(cleanJobId).orElse(null);
            if (jobPosting == null) {
                return ActionResult.failure("The selected job could not be found.");
            }
            if (!Objects.equals(jobPosting.getOwnerId(), actor.getUserId())) {
                return ActionResult.failure("You can only manage jobs that you own.");
            }
            if (jobPosting.getStatus() == targetStatus) {
                return ActionResult.success("Job " + cleanJobId + " is already " + targetStatus.getLabel().toLowerCase() + '.');
            }
            if (targetStatus == JobStatus.OPEN) {
                if (jobPosting.isExpired()) {
                    return ActionResult.failure("This job cannot be reopened because its deadline has already passed.");
                }
                if (countAcceptedApplications(cleanJobId) >= jobPosting.getQuota()) {
                    return ActionResult.failure("This job cannot be reopened because the quota is already full.");
                }
            }

            jobPosting.setStatus(targetStatus);
            jobRepository.save(jobPosting);
            auditRepository.append(
                    actor.getUserId(),
                    "CHANGE_JOB_STATUS",
                    cleanJobId,
                    "SUCCESS",
                    targetStatus.getCode());

            if (targetStatus == JobStatus.CLOSED) {
                return ActionResult.success("Applications are now closed for job " + cleanJobId + '.');
            }
            return ActionResult.success("Job " + cleanJobId + " has been reopened for applications.");
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<JobPosting> synchronizeOperationalStates(List<JobPosting> jobs) {
        Map<String, Long> acceptedCountsByJobId = applicationRepository.findAll().stream()
                .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                .collect(Collectors.groupingBy(ApplicationRecord::getJobId, Collectors.counting()));

        boolean changed = false;
        for (JobPosting job : jobs) {
            long acceptedCount = acceptedCountsByJobId.getOrDefault(job.getJobId(), 0L);
            if (job.isOpen() && (job.isExpired() || acceptedCount >= job.getQuota())) {
                job.setStatus(JobStatus.CLOSED);
                jobRepository.save(job);
                changed = true;
            }
        }
        return changed ? jobRepository.findAll() : jobs;
    }

    private long countAcceptedApplications(String jobId) {
        return applicationRepository.findByJobId(jobId).stream()
                .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                .count();
    }

    private ValidatedJobInput validateJobInput(
            String title,
            String moduleCode,
            String description,
            String requiredSkillsRaw,
            String preferredSkillsRaw,
            String deadline,
            String quotaRaw,
            String workloadHoursRaw) {
        String cleanTitle = cleanText(title, 120);
        String cleanModuleCode = cleanText(moduleCode, 24).toUpperCase().replaceAll("\\s+", "");
        String cleanDescription = cleanText(description, 2000);
        String cleanDeadline = clean(deadline);
        List<String> requiredSkills = parseSkills(requiredSkillsRaw);
        List<String> preferredSkills = parseSkills(preferredSkillsRaw);

        if (cleanTitle.isBlank() || cleanModuleCode.isBlank() || cleanDescription.isBlank() || cleanDeadline.isBlank()) {
            throw new IllegalArgumentException("Title, module code, description and deadline are all required.");
        }
        if (requiredSkills.isEmpty()) {
            throw new IllegalArgumentException("Please provide at least one required skill.");
        }

        LocalDate deadlineDate;
        try {
            deadlineDate = LocalDate.parse(cleanDeadline);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Please enter a valid deadline date.");
        }
        if (deadlineDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Deadline cannot be in the past.");
        }

        int quota = parsePositiveInt(quotaRaw, "quota");
        int workloadHours = parsePositiveInt(workloadHoursRaw, "workload hours");
        return new ValidatedJobInput(
                cleanTitle,
                cleanModuleCode,
                cleanDescription,
                requiredSkills,
                preferredSkills,
                cleanDeadline,
                quota,
                workloadHours);
    }

    private void applyJobInput(JobPosting jobPosting, ValidatedJobInput input) {
        jobPosting.setTitle(input.title());
        jobPosting.setModuleCode(input.moduleCode());
        jobPosting.setDescription(input.description());
        jobPosting.setRequiredSkills(input.requiredSkills());
        jobPosting.setPreferredSkills(input.preferredSkills());
        jobPosting.setDeadline(input.deadline());
        jobPosting.setQuota(input.quota());
        jobPosting.setWorkloadHours(input.workloadHours());
    }

    private List<String> parseSkills(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        return rawValue.lines()
                .flatMap(line -> Arrays.stream(line.split("[,;]")))
                .map(skill -> cleanText(skill, 60))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private int parsePositiveInt(String rawValue, String fieldName) {
        try {
            int parsed = Integer.parseInt(clean(rawValue));
            if (parsed <= 0) {
                throw new IllegalArgumentException("Please enter a positive number for " + fieldName + '.');
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Please enter a valid number for " + fieldName + '.');
        }
    }

    private String clean(String rawValue) {
        return rawValue == null ? "" : rawValue.trim();
    }

    private String cleanText(String rawValue, int maxLength) {
        if (rawValue == null) {
            return "";
        }
        String cleaned = rawValue
                .replace('<', ' ')
                .replace('>', ' ')
                .replaceAll("[\\p{Cntrl}&&[^\\n\\r\\t]]", " ")
                .replace("\r", "")
                .trim();
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, maxLength).trim();
    }

    private record ValidatedJobInput(
            String title,
            String moduleCode,
            String description,
            List<String> requiredSkills,
            List<String> preferredSkills,
            String deadline,
            int quota,
            int workloadHours) {
    }
}
