package com.recruitassist.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/logout")
public class LogoutServlet extends AppServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        HttpSession newSession = req.getSession(true);
        newSession.setAttribute("flashTone", "success");
        newSession.setAttribute("flashMessage", "You have been signed out.");
        redirect(req, resp, "/login");
    }
}
