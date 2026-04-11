package com.recruitassist.web;

import com.recruitassist.model.ActionResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends AppServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (currentUser(req) != null) {
            redirect(req, resp, "/dashboard");
            return;
            
        }
        moveFlashToRequest(req);
        req.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (currentUser(req) != null) {
            redirect(req, resp, "/dashboard");
            return;
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");
        String role = req.getParameter("role");
        String name = req.getParameter("name");
        String email = req.getParameter("email");

        ActionResult result = services(req).userService().registerUser(
                username, password, confirmPassword, role, name, email,
                services(req).idCounterRepository());

        if (!result.isSuccess()) {
            req.setAttribute("error", result.getMessage());
            req.setAttribute("username", username);
            req.setAttribute("name", name);
            req.setAttribute("email", email);
            req.setAttribute("selectedRole", role);
            req.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(req, resp);
            return;
        }

        setFlash(req, "success", result.getMessage());
        redirect(req, resp, "/login");
    }
}
