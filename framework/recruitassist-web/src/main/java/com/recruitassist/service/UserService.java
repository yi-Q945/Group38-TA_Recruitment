package com.recruitassist.service;

import com.recruitassist.model.ActionResult;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.UserRole;
import com.recruitassist.repository.UserRepository;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UserService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserProfile> listAllUsers() {
        return userRepository.findAll();
    }

    public List<UserProfile> listUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    public Optional<UserProfile> findById(String userId) {
        return userRepository.findById(userId);
    }

    public Optional<UserProfile> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Map<String, UserProfile> indexById() {
        return userRepository.findAll().stream()
                .collect(Collectors.toMap(
                        UserProfile::getUserId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    public ActionResult updateTaProfile(
            UserProfile actor,
            String name,
            String studentId,
            String email,
            String programme,
            String skillsRaw,
            String availability,
            String experience,
            String cvText) {
        if (actor == null || actor.getRole() != UserRole.TA) {
            return ActionResult.failure("Only teaching assistants can edit applicant profiles.");
        }

        String cleanName = cleanText(name, 80);
        String cleanStudentId = cleanText(studentId, 32).replaceAll("\\s+", "");
        String cleanEmail = cleanText(email, 120).toLowerCase();
        String cleanProgramme = cleanText(programme, 120);
        String cleanAvailability = cleanText(availability, 180);
        String cleanExperience = cleanText(experience, 2000);
        String cleanCvText = cleanText(cvText, 4000);
        List<String> skills = parseSkills(skillsRaw);

        if (cleanName.isBlank() || cleanStudentId.isBlank() || cleanProgramme.isBlank()) {
            return ActionResult.failure("Name, student ID and programme are required.");
        }
        if (skills.isEmpty()) {
            return ActionResult.failure("Please provide at least one skill for matching.");
        }
        if (cleanAvailability.isBlank()) {
            return ActionResult.failure("Please describe your availability so MOs can schedule you.");
        }
        if (!cleanEmail.isBlank() && !EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            return ActionResult.failure("Please provide a valid email address or leave the email field empty.");
        }

        actor.setName(cleanName);
        actor.setStudentId(cleanStudentId);
        actor.setEmail(cleanEmail);
        actor.setProgramme(cleanProgramme);
        actor.setSkills(skills);
        actor.setAvailability(cleanAvailability);
        actor.setExperience(cleanExperience);
        actor.setCvText(cleanCvText);
        save(actor);
        return ActionResult.success("Your applicant profile has been updated.");
    }

    public ActionResult updateCvMetadata(UserProfile actor, String storedFileName) {
        if (actor == null || actor.getRole() != UserRole.TA) {
            return ActionResult.failure("Only teaching assistants can upload CV files.");
        }
        actor.setCvFileName(cleanText(storedFileName, 180));
        actor.setCvUploadedAt(Instant.now().toString());
        save(actor);
        return ActionResult.success("CV uploaded successfully.");
    }

    public void save(UserProfile userProfile) {
        userRepository.save(userProfile);
    }

    private List<String> parseSkills(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        return rawValue.lines()
                .flatMap(line -> Arrays.stream(line.split("[,;]")))
                .map(skill -> cleanText(skill, 60))
                .filter(skill -> !skill.isBlank())
                .distinct()
                .toList();
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
}
