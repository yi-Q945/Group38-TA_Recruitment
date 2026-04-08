package com.recruitassist.web;

import com.recruitassist.model.ActionResult;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.UserRole;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/jobs/create")
public class CreateJobServlet extends AppServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UserProfile user = requireAuthenticatedUser(req, resp);
        if (user == null || !requireRole(user, UserRole.MO, req, resp)) {
            return;
        }

        ActionResult result = services(req).jobService().createJob(
                user,
                req.getParameter("title"),
                req.getParameter("moduleCode"),
                req.getParameter("description"),
                req.getParameter("requiredSkills"),
                req.getParameter("preferredSkills"),
                req.getParameter("deadline"),
                req.getParameter("quota"),
                req.getParameter("workloadHours"));

        setFlash(req, result.isSuccess() ? "success" : "error", result.getMessage());
        redirect(req, resp, "/dashboard");
    }
}
