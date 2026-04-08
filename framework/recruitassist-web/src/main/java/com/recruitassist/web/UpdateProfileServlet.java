package com.recruitassist.web;

import com.recruitassist.model.ActionResult;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.UserRole;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/profile/update")
public class UpdateProfileServlet extends AppServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserProfile user = requireAuthenticatedUser(req, resp);
        if (user == null || !requireRole(user, UserRole.TA, req, resp)) {
            return;
        }

        ActionResult result = services(req).userService().updateTaProfile(
                user,
                req.getParameter("name"),
                req.getParameter("studentId"),
                req.getParameter("email"),
                req.getParameter("programme"),
                req.getParameter("skills"),
                req.getParameter("availability"),
                req.getParameter("experience"),
                req.getParameter("cvText"));
        setFlash(req, result.isSuccess() ? "success" : "error", result.getMessage());
        redirect(req, resp, "/dashboard");
    }
}
