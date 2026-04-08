package com.recruitassist.web;

import com.recruitassist.config.AppContextKeys;
import com.recruitassist.config.AppServices;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.UserRole;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

public abstract class AppServlet extends HttpServlet {
    protected static final String SESSION_USER_ID = "sessionUserId";
    private static final String FLASH_TONE = "flashTone";
    private static final String FLASH_MESSAGE = "flashMessage";

    protected AppServices services(HttpServletRequest request) {
        return (AppServices) getServletContext().getAttribute(AppContextKeys.SERVICES);
    }

    protected UserProfile currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        String userId = (String) session.getAttribute(SESSION_USER_ID);
        if (userId == null || userId.isBlank()) {
            return null;
        }

        return services(request).userService().findById(userId).orElse(null);
    }

    protected UserProfile requireAuthenticatedUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        UserProfile user = currentUser(request);
        if (user == null) {
            redirect(request, response, "/login");
        }
        return user;
    }

    protected boolean requireRole(
            UserProfile user,
            UserRole expectedRole,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (user == null || user.getRole() != expectedRole) {
            setFlash(request, "error", "You do not have access to that action.");
            redirect(request, response, "/dashboard");
            return false;
        }
        return true;
    }

    protected void setFlash(HttpServletRequest request, String tone, String message) {
        HttpSession session = request.getSession(true);
        session.setAttribute(FLASH_TONE, tone);
        session.setAttribute(FLASH_MESSAGE, message);
    }

    protected void moveFlashToRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }

        Object tone = session.getAttribute(FLASH_TONE);
        Object message = session.getAttribute(FLASH_MESSAGE);
        if (tone != null) {
            request.setAttribute("flashTone", tone);
            session.removeAttribute(FLASH_TONE);
        }
        if (message != null) {
            request.setAttribute("flashMessage", message);
            session.removeAttribute(FLASH_MESSAGE);
        }
    }

    protected void redirect(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }
}
