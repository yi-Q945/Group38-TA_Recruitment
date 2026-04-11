package com.recruitassist.web;

import com.recruitassist.config.AppPaths;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.UserRole;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/home")
public class HomeServlet extends AppServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (currentUser(req) != null) {
            redirect(req, resp, "/dashboard");
            return;
        }

        List<UserProfile> allUsers = services(req).userService().listAllUsers();
        long taCount = allUsers.stream().filter(user -> user.getRole() == UserRole.TA).count();
        long recruiterCount = allUsers.stream().filter(user -> user.getRole() == UserRole.MO).count();
        long adminCount = allUsers.stream().filter(user -> user.getRole() == UserRole.ADMIN).count();
        int jobCount = services(req).jobService().listAllJobs().size();
        int applicationCount = services(req).applicationService().findAll().size();

        req.setAttribute("appName", services(req).systemConfig().getAppName());
        req.setAttribute("stack", "Java 17 + Maven + Servlet/JSP + JSON/CSV/TXT");
        req.setAttribute("frameworkDir", AppPaths.frameworkDir().toString());
        req.setAttribute("dataDir", AppPaths.dataDir().toString());
        req.setAttribute("logsDir", AppPaths.logsDir().toString());
        req.setAttribute("demoPassword", "demo123");
        req.setAttribute("taCount", taCount);
        req.setAttribute("recruiterCount", recruiterCount);
        req.setAttribute("adminCount", adminCount);
        req.setAttribute("jobCount", jobCount);
        req.setAttribute("applicationCount", applicationCount);
        req.setAttribute("featuredDemoUsers", buildFeaturedUsers(allUsers));
        req.getRequestDispatcher("/WEB-INF/jsp/home.jsp").forward(req, resp);
    }

    private List<UserProfile> buildFeaturedUsers(List<UserProfile> allUsers) {
        List<UserProfile> featuredUsers = new ArrayList<>();
        allUsers.stream().filter(user -> user.getRole() == UserRole.TA).findFirst().ifPresent(featuredUsers::add);
        allUsers.stream().filter(user -> user.getRole() == UserRole.MO).findFirst().ifPresent(featuredUsers::add);
        allUsers.stream().filter(user -> user.getRole() == UserRole.ADMIN).findFirst().ifPresent(featuredUsers::add);
        return featuredUsers;
    }
}
