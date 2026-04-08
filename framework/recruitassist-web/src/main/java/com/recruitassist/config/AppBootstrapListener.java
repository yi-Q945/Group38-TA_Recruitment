package com.recruitassist.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppBootstrapListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            AppPaths.ensureBaseStructure();
            AppServices services = new AppServices();
            sce.getServletContext().setAttribute(AppContextKeys.SERVICES, services);
            sce.getServletContext().setAttribute("appName", services.systemConfig().getAppName());
            sce.getServletContext().setAttribute("frameworkDir", AppPaths.frameworkDir().toString());
            sce.getServletContext().setAttribute("dataDir", AppPaths.dataDir().toString());
            sce.getServletContext().setAttribute("logsDir", AppPaths.logsDir().toString());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to prepare base directories", ex);
        }
    }
}
