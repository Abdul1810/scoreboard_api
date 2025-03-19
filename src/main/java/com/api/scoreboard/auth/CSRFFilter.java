package com.api.scoreboard.auth;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter("/api/stats/*")
public class CSRFFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, JSESSIONID, X-CSRF-TOKEN");
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
        httpResponse.setHeader("Content-Type", "application/json");
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        HttpSession session = req.getSession(false);
        System.out.println("Session ID: " + session.getId());
        System.out.println("Authenticated: " + session.getAttribute("authenticated"));
        System.out.println("CSRF Token: " + session.getAttribute("csrfToken"));

        if (session.getAttribute("authenticated") != null && (boolean) session.getAttribute("authenticated")) {
            String csrfTokenFromSession = (String) session.getAttribute("csrfToken");
            String csrfTokenFromHeader = req.getHeader("X-CSRF-TOKEN");
            System.out.println("CSRF Token from session: " + csrfTokenFromSession);
            System.out.println("CSRF Token from header: " + csrfTokenFromHeader);

            if (csrfTokenFromSession != null && csrfTokenFromSession.equals(csrfTokenFromHeader)) {
                chain.doFilter(request, response);
            } else {
                ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("CSRF token validation failed");
            }
        } else {
            ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("No valid session found");
        }
    }
}


