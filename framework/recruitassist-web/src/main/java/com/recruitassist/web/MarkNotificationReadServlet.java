package com.recruitassist.web;

import com.recruitassist.model.ActionResult;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.UserRole;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/notifications/read")
public class MarkNotificationReadServlet extends AppServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UserProfile user = requireAuthenticatedUser(req, resp);
        if (user == null || !requireRole(user, UserRole.TA, req, resp)) {
            return;
        }

        ActionResult result = services(req).notificationService()
                .markAsRead(user, req.getParameter("notificationId"));
        setFlash(req, result.isSuccess() ? "success" : "error", result.getMessage());
        redirect(req, resp, "/dashboard");
    }
}
