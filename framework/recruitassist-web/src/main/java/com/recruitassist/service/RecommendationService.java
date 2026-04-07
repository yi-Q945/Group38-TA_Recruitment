package com.recruitassist.service;

import com.recruitassist.model.ApplicationRecord;
import com.recruitassist.model.ApplicationStatus;
import com.recruitassist.model.JobPosting;
import com.recruitassist.model.SystemConfig;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.view.JobRecommendation;
import com.recruitassist.repository.ApplicationRepository;
import com.recruitassist.repository.JobRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecommendationService {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-z0-9]{2,}");
    private static final Set<String> STOP_WORDS = Set.of(
            "the",
            "and",
            "for",
            "with",
            "that",
            "this",
            "will",
            "into",
            "from",
            "your",
            "their",
            "during",
            "while",
            "able",
            "need",
            "role",
            "support",
            "assist",
            "help",
            "provide",
            "using",
            "used",
            "weekly",
            "students",
            "student",
            "teaching",
            "assistant",
            "module",
            "sessions",
            "session");
    private static final Set<String> EVIDENCE_KEYWORDS = Set.of(
            "lab",
            "labs",
            "marking",
            "marked",
            "assessment",
            "assessments",
            "debugging",
            "debug",
            "tutoring",
            "tutor",
            "mentoring",
            "mentor",
            "invigilation",
            "invigilate",
            "communication",
            "organisation",
            "support",
            "teaching",
            "quizzes",
            "coursework");
    private static final Map<String, String> SKILL_ALIASES = buildSkillAliases();

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final WorkloadService workloadService;
    private final SystemConfig systemConfig;

    public RecommendationService(
            JobRepository jobRepository,
            ApplicationRepository applicationRepository,
            WorkloadService workloadService,
            SystemConfig systemConfig) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.workloadService = workloadService;
        this.systemConfig = systemConfig;
    }

    public List<JobRecommendation> recommendJobsFor(UserProfile user) {
        return jobRepository.findAll().stream()
                .filter(JobPosting::isOpen)
                .map(job -> recommend(user, job))
                .sorted(Comparator.comparingDouble(JobRecommendation::getScore).reversed())
                .toList();
    }

    public JobRecommendation recommend(UserProfile user, JobPosting job) {
        SkillProfile skillProfile = buildSkillProfile(user);
        String profileText = buildProfileText(user);
        Set<String> profileTokens = tokenize(profileText);
        Set<String> jobKeywords = jobKeywords(job);

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();
        for (String skill : job.getRequiredSkills()) {
            if (matchesSkill(skillProfile, skill, profileTokens)) {
                matchedSkills.add(skill);
            } else {
                missingSkills.add(skill);
            }
        }

        List<String> preferredMatches = job.getPreferredSkills().stream()
                .filter(skill -> matchesSkill(skillProfile, skill, profileTokens))
                .toList();

        int requiredTotal = job.getRequiredSkills().size();
        int preferredTotal = job.getPreferredSkills().size();
        int currentWorkload = workloadService.workloadForUser(user.getUserId());
        int workloadThreshold = Math.max(1, workloadService.getThreshold());
        int projectedWorkload = currentWorkload + Math.max(job.getWorkloadHours(), 0);

        double skillScore = calculateSkillScore(skillProfile, matchedSkills, preferredMatches, missingSkills, requiredTotal, preferredTotal);
        double availabilityScore = calculateAvailabilityScore(user.getAvailability());
        double experienceScore = calculateExperienceScore(user, job, profileTokens, jobKeywords, matchedSkills);
        double workloadBalance = calculateWorkloadBalance(currentWorkload, job.getWorkloadHours(), workloadThreshold);
        double profileEvidenceScore = calculateProfileEvidenceScore(user, job, matchedSkills, preferredMatches, profileTokens);
        CompetitionSnapshot competition = calculateCompetition(job);

        SystemConfig.RecommendationConfig weights = systemConfig.getRecommendation();
        double configuredTotal = weights.getSkillMatchWeight()
                + weights.getAvailabilityWeight()
                + weights.getExperienceWeight()
                + weights.getWorkloadBalanceWeight()
                + weights.getProfileEvidenceWeight()
                + weights.getCompetitionWeight();
        double safeTotal = configuredTotal <= 0.0 ? 1.0 : configuredTotal;

        double score = ((skillScore * weights.getSkillMatchWeight())
                + (availabilityScore * weights.getAvailabilityWeight())
                + (experienceScore * weights.getExperienceWeight())
                + (workloadBalance * weights.getWorkloadBalanceWeight())
                + (profileEvidenceScore * weights.getProfileEvidenceWeight())
                + (competition.score() * weights.getCompetitionWeight())) / safeTotal;
        score = clamp(score);

        List<String> evidenceHits = collectEvidenceHits(user, job, matchedSkills, preferredMatches, profileTokens);
        List<String> reasons = new ArrayList<>();
        reasons.add(buildSkillReason(matchedSkills, preferredMatches, missingSkills, requiredTotal, preferredTotal));
        reasons.add(buildAvailabilityReason(user.getAvailability(), availabilityScore));
        reasons.add(buildExperienceReason(user, experienceScore, evidenceHits));
        reasons.add(buildProfileEvidenceReason(profileEvidenceScore, user.isCvAvailable(), evidenceHits));
        reasons.add(buildWorkloadReason(currentWorkload, projectedWorkload, workloadThreshold));
        reasons.add(buildCompetitionReason(competition));

        return new JobRecommendation(
                job,
                score,
                matchedSkills,
                missingSkills,
                preferredMatches,
                reasons,
                skillScore,
                availabilityScore,
                experienceScore,
                workloadBalance,
                profileEvidenceScore,
                competition.score(),
                currentWorkload,
                projectedWorkload,
                workloadThreshold,
                competition.activeApplicants(),
                competition.remainingSlots());
    }

    private SkillProfile buildSkillProfile(UserProfile user) {
        LinkedHashSet<String> canonicalSkills = new LinkedHashSet<>();
        List<String> declaredSkills = new ArrayList<>();
        for (String skill : user.getSkills()) {
            String normalized = normalizePhrase(skill);
            if (normalized.isBlank()) {
                continue;
            }
            declaredSkills.add(normalized);
            canonicalSkills.add(canonicalizeSkill(normalized));
        }
        return new SkillProfile(canonicalSkills, declaredSkills);
    }

    private boolean matchesSkill(SkillProfile skillProfile, String jobSkill, Set<String> profileTokens) {
        String canonicalJobSkill = canonicalizeSkill(jobSkill);
        if (skillProfile.canonicalSkills().contains(canonicalJobSkill)) {
            return true;
        }

        Set<String> jobTokens = tokenize(canonicalJobSkill);
        for (String declaredSkill : skillProfile.declaredSkills()) {
            if (jaccard(jobTokens, tokenize(canonicalizeSkill(declaredSkill))) >= 0.55) {
                return true;
            }
        }
        return !jobTokens.isEmpty() && profileTokens.containsAll(jobTokens);
    }

    private double calculateSkillScore(
            SkillProfile skillProfile,
            List<String> matchedSkills,
            List<String> preferredMatches,
            List<String> missingSkills,
            int requiredTotal,
            int preferredTotal) {
        double requiredCoverage = requiredTotal == 0 ? 0.8 : matchedSkills.size() / (double) requiredTotal;
        double preferredCoverage = preferredTotal == 0 ? 0.55 : preferredMatches.size() / (double) preferredTotal;
        double breadthBonus = Math.min((skillProfile.canonicalSkills().size() * 0.02), 0.12);
        double gapPenalty = missingSkills.isEmpty() ? 0.0 : Math.min(missingSkills.size() * 0.06, 0.18);
        double fullCoverageBonus = requiredTotal > 0 && matchedSkills.size() == requiredTotal ? 0.08 : 0.0;
        return clamp((requiredCoverage * 0.72) + (preferredCoverage * 0.18) + breadthBonus + fullCoverageBonus - gapPenalty);
    }

    private double calculateAvailabilityScore(String availability) {
        String normalizedAvailability = normalizeText(availability);
        if (normalizedAvailability.isBlank()) {
            return 0.28;
        }

        double score = 0.34;
        if (containsAny(normalizedAvailability, "weekday", "weekdays", "weekend", "weekends", "flexible")) {
            score += 0.18;
        }
        if (containsAny(
                normalizedAvailability,
                "monday",
                "tuesday",
                "wednesday",
                "thursday",
                "friday",
                "saturday",
                "sunday",
                "mon",
                "tue",
                "wed",
                "thu",
                "fri",
                "sat",
                "sun")) {
            score += 0.22;
        }
        if (containsAny(normalizedAvailability, "morning", "afternoon", "evening", "night", "am", "pm")) {
            score += 0.16;
        }
        if (availability.contains("/") || availability.contains(",") || availability.contains(";")) {
            score += 0.08;
        }
        return clamp(score);
    }

    private double calculateExperienceScore(
            UserProfile user,
            JobPosting job,
            Set<String> profileTokens,
            Set<String> jobKeywords,
            List<String> matchedSkills) {
        String profileText = buildProfileText(user);
        if (profileText.isBlank()) {
            return 0.26;
        }

        double score = 0.3;
        if (!user.getExperience().isBlank()) {
            score += 0.14;
        }
        if (!user.getCvText().isBlank() || user.isCvAvailable()) {
            score += 0.12;
        }

        double phraseCoverage = phraseCoverage(job, profileText);
        double tokenCoverage = tokenOverlap(jobKeywords, profileTokens);
        double evidenceCoverage = evidenceKeywordCoverage(profileText, job);
        double matchedSkillSupport = Math.min(matchedSkills.size() * 0.05, 0.16);

        score += phraseCoverage * 0.16;
        score += tokenCoverage * 0.16;
        score += evidenceCoverage * 0.14;
        score += matchedSkillSupport;
        return clamp(score);
    }

    private double calculateProfileEvidenceScore(
            UserProfile user,
            JobPosting job,
            List<String> matchedSkills,
            List<String> preferredMatches,
            Set<String> profileTokens) {
        double completeness = 0.0;
        if (!user.getSkills().isEmpty()) {
            completeness += 0.28;
        }
        if (!user.getAvailability().isBlank()) {
            completeness += 0.2;
        }
        if (!user.getExperience().isBlank()) {
            completeness += 0.2;
        }
        if (!user.getCvText().isBlank()) {
            completeness += 0.18;
        }
        if (user.isCvAvailable()) {
            completeness += 0.14;
        }

        double jobAlignment = tokenOverlap(jobKeywords(job), profileTokens);
        double breadthSignal = Math.min((matchedSkills.size() * 0.07) + (preferredMatches.size() * 0.05), 0.28);
        return clamp((completeness * 0.58) + (jobAlignment * 0.22) + breadthSignal);
    }

    private double calculateWorkloadBalance(int currentWorkload, int jobWorkloadHours, int workloadThreshold) {
        int projectedWorkload = currentWorkload + Math.max(jobWorkloadHours, 0);
        if (currentWorkload >= workloadThreshold) {
            return 0.1;
        }
        if (projectedWorkload <= workloadThreshold) {
            double remainingRatio = Math.max(workloadThreshold - projectedWorkload, 0) / (double) workloadThreshold;
            return clamp(0.55 + (remainingRatio * 0.45));
        }
        double overBy = projectedWorkload - workloadThreshold;
        double penaltyRatio = overBy / (double) workloadThreshold;
        return clamp(0.36 - (penaltyRatio * 0.48));
    }

    private CompetitionSnapshot calculateCompetition(JobPosting job) {
        List<ApplicationRecord> activeApplications = applicationRepository.findByJobId(job.getJobId()).stream()
                .filter(application -> application.getStatus() != ApplicationStatus.WITHDRAWN)
                .filter(application -> application.getStatus() != ApplicationStatus.REJECTED)
                .toList();
        int acceptedCount = (int) activeApplications.stream()
                .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                .count();
        int remainingSlots = Math.max(job.getQuota() - acceptedCount, 0);

        if (remainingSlots <= 0) {
            return new CompetitionSnapshot(activeApplications.size(), 0, 0.05);
        }

        double activePerSlot = activeApplications.size() / (double) remainingSlots;
        double score;
        if (activePerSlot <= 1.0) {
            score = 0.95;
        } else if (activePerSlot <= 2.0) {
            score = 0.82;
        } else if (activePerSlot <= 3.0) {
            score = 0.68;
        } else if (activePerSlot <= 4.0) {
            score = 0.54;
        } else {
            score = Math.max(0.18, 0.54 - ((activePerSlot - 4.0) * 0.07));
        }
        return new CompetitionSnapshot(activeApplications.size(), remainingSlots, clamp(score));
    }

    private List<String> collectEvidenceHits(
            UserProfile user,
            JobPosting job,
            List<String> matchedSkills,
            List<String> preferredMatches,
            Set<String> profileTokens) {
        LinkedHashSet<String> hits = new LinkedHashSet<>();
        for (String skill : matchedSkills) {
            hits.add(skill);
            if (hits.size() >= 4) {
                return hits.stream().limit(4).toList();
            }
        }
        for (String skill : preferredMatches) {
            hits.add(skill);
            if (hits.size() >= 4) {
                return hits.stream().limit(4).toList();
            }
        }
        String profileText = buildProfileText(user);
        for (String keyword : EVIDENCE_KEYWORDS) {
            if (hits.size() >= 4) {
                break;
            }
            if (profileText.contains(keyword) && (normalizeText(job.getDescription()).contains(keyword) || profileTokens.contains(keyword))) {
                hits.add(keyword);
            }
        }
        return hits.stream().limit(4).toList();
    }

    private String buildSkillReason(
            List<String> matchedSkills,
            List<String> preferredMatches,
            List<String> missingSkills,
            int requiredTotal,
            int preferredTotal) {
        StringBuilder builder = new StringBuilder();
        builder.append("Required skill coverage: ")
                .append(matchedSkills.size())
                .append('/')
                .append(requiredTotal)
                .append('.');

        if (preferredTotal > 0) {
            builder.append(" Preferred alignment: ")
                    .append(preferredMatches.size())
                    .append('/')
                    .append(preferredTotal)
                    .append('.');
        } else {
            builder.append(" No preferred-skill constraints were configured.");
        }

        if (!missingSkills.isEmpty()) {
            builder.append(" Skills to strengthen next: ")
                    .append(String.join(", ", missingSkills))
                    .append('.');
        }
        return builder.toString();
    }

    private String buildAvailabilityReason(String availability, double availabilityScore) {
        if (availability == null || availability.isBlank()) {
            return "Availability details are missing, so scheduling confidence stays conservative.";
        }
        if (availabilityScore >= 0.75) {
            return "Availability is detailed enough for planning: " + availability + '.';
        }
        return "Availability is present but could be more specific for scheduling decisions: " + availability + '.';
    }

    private String buildExperienceReason(UserProfile user, double experienceScore, List<String> evidenceHits) {
        if (user.getExperience().isBlank() && user.getCvText().isBlank() && !user.isCvAvailable()) {
            return "Experience and CV evidence are sparse, which limits confidence in role readiness.";
        }
        if (!evidenceHits.isEmpty()) {
            return "Profile evidence aligns with the role through " + String.join(", ", evidenceHits)
                    + ", lifting the experience score.";
        }
        if (experienceScore >= 0.7) {
            return "Experience evidence is well aligned with this role, even without many exact phrase matches.";
        }
        return "Some experience evidence exists, but the profile could mention more role-specific examples.";
    }

    private String buildProfileEvidenceReason(double profileEvidenceScore, boolean hasCvFile, List<String> evidenceHits) {
        if (profileEvidenceScore >= 0.75) {
            return hasCvFile
                    ? "Profile evidence is strong: structured fields, CV upload and skill signals are all available."
                    : "Profile evidence is strong and consistent with the job signals.";
        }
        if (profileEvidenceScore >= 0.5) {
            return !evidenceHits.isEmpty()
                    ? "Profile evidence is usable, and a few strong signals are already visible."
                    : "Profile evidence is usable, but more concrete examples would improve explainability.";
        }
        return "Profile evidence is still thin, so the recommendation remains cautious.";
    }

    private String buildWorkloadReason(int currentWorkload, int projectedWorkload, int workloadThreshold) {
        if (projectedWorkload > workloadThreshold) {
            return "Accepting this role would move workload from " + currentWorkload + "h to " + projectedWorkload
                    + "h, above the " + workloadThreshold + "h threshold.";
        }
        return "Current accepted workload is " + currentWorkload + "h; this role would project to " + projectedWorkload
                + "h within the " + workloadThreshold + "h threshold.";
    }

    private String buildCompetitionReason(CompetitionSnapshot competition) {
        if (competition.remainingSlots() <= 0) {
            return "All quota is currently allocated, so competition pressure is extremely high.";
        }
        if (competition.score() >= 0.8) {
            return "Demand pressure is manageable: " + competition.activeApplicants() + " active applicants for "
                    + competition.remainingSlots() + " remaining slot" + (competition.remainingSlots() == 1 ? "" : "s") + '.';
        }
        return "Competition is heavier here: " + competition.activeApplicants() + " active applicants for "
                + competition.remainingSlots() + " remaining slot" + (competition.remainingSlots() == 1 ? "" : "s") + '.';
    }

    private double phraseCoverage(JobPosting job, String profileText) {
        if (profileText.isBlank()) {
            return 0.0;
        }
        LinkedHashSet<String> phrases = new LinkedHashSet<>();
        phrases.add(normalizePhrase(job.getTitle()));
        phrases.add(normalizePhrase(job.getModuleCode()));
        job.getRequiredSkills().stream().map(this::canonicalizeSkill).forEach(phrases::add);
        job.getPreferredSkills().stream().map(this::canonicalizeSkill).forEach(phrases::add);

        long relevantCount = phrases.stream().filter(phrase -> !phrase.isBlank()).count();
        if (relevantCount == 0) {
            return 0.0;
        }

        long matches = phrases.stream()
                .filter(phrase -> !phrase.isBlank())
                .filter(profileText::contains)
                .count();
        return clamp(matches / (double) relevantCount);
    }

    private double evidenceKeywordCoverage(String profileText, JobPosting job) {
        Set<String> profileTokens = tokenize(profileText);
        Set<String> jobTokens = tokenize(job.getDescription());
        if (profileTokens.isEmpty() || jobTokens.isEmpty()) {
            return 0.0;
        }
        long matched = EVIDENCE_KEYWORDS.stream()
                .filter(profileTokens::contains)
                .filter(jobTokens::contains)
                .count();
        return clamp(matched / 4.0);
    }

    private Set<String> jobKeywords(JobPosting job) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        tokens.addAll(tokenize(job.getTitle()));
        tokens.addAll(tokenize(job.getModuleCode()));
        tokens.addAll(tokenize(job.getDescription()));
        job.getRequiredSkills().forEach(skill -> tokens.addAll(tokenize(canonicalizeSkill(skill))));
        job.getPreferredSkills().forEach(skill -> tokens.addAll(tokenize(canonicalizeSkill(skill))));
        tokens.removeIf(STOP_WORDS::contains);
        return tokens;
    }

    private double tokenOverlap(Set<String> referenceTokens, Set<String> profileTokens) {
        if (referenceTokens.isEmpty() || profileTokens.isEmpty()) {
            return 0.0;
        }
        long matched = referenceTokens.stream().filter(profileTokens::contains).count();
        double denominator = Math.max(4, Math.min(referenceTokens.size(), 12));
        return clamp(matched / denominator);
    }

    private double jaccard(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        long intersection = left.stream().filter(right::contains).count();
        long union = left.size() + right.size() - intersection;
        return union == 0 ? 0.0 : intersection / (double) union;
    }

    private Set<String> tokenize(String rawText) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        Matcher matcher = TOKEN_PATTERN.matcher(normalizeText(rawText));
        while (matcher.find()) {
            String token = matcher.group();
            if (!STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String buildProfileText(UserProfile user) {
        return normalizeText(
                user.getProgramme() + ' '
                        + user.getAvailability() + ' '
                        + user.getExperience() + ' '
                        + user.getCvText() + ' '
                        + user.getSkillsSummary());
    }

    private String canonicalizeSkill(String rawSkill) {
        String normalized = normalizePhrase(rawSkill);
        return SKILL_ALIASES.getOrDefault(normalized, normalized);
    }

    private String normalizePhrase(String rawValue) {
        return normalizeText(rawValue)
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeText(String rawValue) {
        return rawValue == null ? "" : rawValue.toLowerCase(Locale.ENGLISH).trim();
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(value, 1.0));
    }

    private static Map<String, String> buildSkillAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        mapAlias(aliases, "oop", "oop", "object oriented programming", "object-oriented programming");
        mapAlias(aliases, "testing", "testing", "unit testing", "junit", "integration testing", "qa");
        mapAlias(aliases, "communication", "communication", "presentation", "explaining", "student support");
        mapAlias(aliases, "marking", "marking", "assessment", "grading", "feedback");
        mapAlias(aliases, "lab support", "lab support", "lab assistance", "lab teaching", "laboratory support");
        mapAlias(aliases, "tutoring", "tutoring", "teaching", "mentoring", "mentor", "peer mentoring");
        mapAlias(aliases, "debugging", "debugging", "troubleshooting", "bug fixing", "debug");
        mapAlias(aliases, "data structures", "data structures", "algorithms", "algorithmic thinking");
        return aliases;
    }

    private static void mapAlias(Map<String, String> aliases, String canonical, String... variants) {
        for (String variant : variants) {
            aliases.put(variant, canonical);
        }
    }

    private record CompetitionSnapshot(int activeApplicants, int remainingSlots, double score) {
    }

    private record SkillProfile(Set<String> canonicalSkills, List<String> declaredSkills) {
    }
}
