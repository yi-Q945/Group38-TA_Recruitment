package com.recruitassist.web;

import com.recruitassist.config.AppPaths;
import com.recruitassist.model.ActionResult;
import com.recruitassist.model.UserProfile;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

@WebServlet("/profile/cv/upload")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class UploadCvServlet extends AppServlet {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "txt");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserProfile user = requireAuthenticatedUser(req, resp);
        if (user == null || user.getRole() != com.recruitassist.model.UserRole.TA) {
            setFlash(req, "error", "Only teaching assistants can upload CV files.");
            redirect(req, resp, "/dashboard");
            return;
        }

        Part cvPart = req.getPart("cvFile");
        if (cvPart == null || cvPart.getSize() <= 0) {
            setFlash(req, "error", "Please choose a CV file to upload.");
            redirect(req, resp, "/dashboard");
            return;
        }
        if (cvPart.getSize() > 5L * 1024 * 1024) {
            setFlash(req, "error", "CV file must be 5MB or smaller.");
            redirect(req, resp, "/dashboard");
            return;
        }

        String submittedFileName = submittedFileName(cvPart);
        String extension = fileExtension(submittedFileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            setFlash(req, "error", "Please upload a PDF, DOC, DOCX or TXT file.");
            redirect(req, resp, "/dashboard");
            return;
        }

        Files.createDirectories(AppPaths.cvDir());
        deleteExistingCvFiles(user.getUserId());
        String storedFileName = user.getUserId() + "_cv." + extension;
        Path targetFile = AppPaths.cvDir().resolve(storedFileName).normalize();
        try (InputStream inputStream = cvPart.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }

        ActionResult result = services(req).userService().updateCvMetadata(user, storedFileName);
        setFlash(req, result.isSuccess() ? "success" : "error", result.getMessage());
        redirect(req, resp, "/dashboard");
    }

    private void deleteExistingCvFiles(String userId) throws IOException {
        try (Stream<Path> paths = Files.list(AppPaths.cvDir())) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(userId + "_cv."))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            throw new IllegalStateException("Failed to replace existing CV file.", ex);
                        }
                    });
        }
    }

    private String submittedFileName(Part part) {
        String fileName = part.getSubmittedFileName();
        return fileName == null ? "" : fileName.replace('\\', '/');
    }

    private String fileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ENGLISH);
    }
}
