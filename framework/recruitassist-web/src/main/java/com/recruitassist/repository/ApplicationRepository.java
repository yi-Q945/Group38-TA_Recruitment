package com.recruitassist.repository;

import com.recruitassist.config.AppPaths;
import com.recruitassist.model.ApplicationRecord;
import com.recruitassist.util.JsonFileStore;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class ApplicationRepository {
    private final JsonFileStore jsonFileStore;

    public ApplicationRepository(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public List<ApplicationRecord> findAll() {
        return jsonFileStore.readAll(AppPaths.applicationsDir(), ApplicationRecord.class).stream()
                .sorted((left, right) -> right.getApplyTime().compareToIgnoreCase(left.getApplyTime()))
                .toList();
    }

    public List<ApplicationRecord> findByApplicantId(String applicantId) {
        return findAll().stream()
                .filter(application -> application.getApplicantId().equalsIgnoreCase(applicantId))
                .toList();
    }

    public List<ApplicationRecord> findByJobId(String jobId) {
        return findAll().stream()
                .filter(application -> application.getJobId().equalsIgnoreCase(jobId))
                .toList();
    }

    public Optional<ApplicationRecord> findById(String applicationId) {
        return findAll().stream()
                .filter(application -> application.getApplicationId().equalsIgnoreCase(applicationId))
                .findFirst();
    }

    public void save(ApplicationRecord application) {
        jsonFileStore.write(filePath(application.getApplicationId()), application);
    }

    private Path filePath(String applicationId) {
        return AppPaths.applicationsDir().resolve(applicationId + ".json");
    }
}
