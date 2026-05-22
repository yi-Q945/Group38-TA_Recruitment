package com.recruitassist.web;

import com.recruitassist.config.AppPaths;
import com.recruitassist.config.AppServices;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Instant;

@WebServlet("/health")
public class HealthServlet extends AppServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            AppPaths.ensureBaseStructure();
            AppServices services = services(req);
            int userCount = services.userService().listAllUsers().size();
            int jobCount = services.jobService().listAllJobs().size();
            int applicationCount = services.applicationService().findAll().size();
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("{"
                    + "\"status\":\"UP\","
                    + "\"timestamp\":\"" + Instant.now() + "\","
                    + "\"users\":" + userCount + ","
                    + "\"jobs\":" + jobCount + ","
                    + "\"applications\":" + applicationCount
                    + "}");
        } catch (RuntimeException ex) {
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            resp.getWriter().write("{\"status\":\"DOWN\"}");
        }
    }
}
