package com.recruitassist.repository;

import com.recruitassist.config.AppPaths;
import com.recruitassist.model.JobPosting;
import com.recruitassist.util.JsonFileStore;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class JobRepository {
    private final JsonFileStore jsonFileStore;

    public JobRepository(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public List<JobPosting> findAll() {
        return jsonFileStore.readAll(AppPaths.jobsDir(), JobPosting.class).stream()
                .sorted((left, right) -> {
                    int deadlineCompare = left.getDeadline().compareToIgnoreCase(right.getDeadline());
                    if (deadlineCompare != 0) {
                        return deadlineCompare;
                    }
                    return left.getTitle().compareToIgnoreCase(right.getTitle());
                })
                .toList();
    }

    public Optional<JobPosting> findById(String jobId) {
        return findAll().stream()
                .filter(job -> job.getJobId().equalsIgnoreCase(jobId))
                .findFirst();
    }

    public List<JobPosting> findByOwnerId(String ownerId) {
        return findAll().stream()
                .filter(job -> job.getOwnerId().equalsIgnoreCase(ownerId))
                .toList();
    }

    public void save(JobPosting jobPosting) {
        jsonFileStore.write(filePath(jobPosting.getJobId()), jobPosting);
    }

    private Path filePath(String jobId) {
        return AppPaths.jobsDir().resolve(jobId + ".json");
    }
}
