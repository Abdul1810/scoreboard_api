package com.api.scoreboard.auth;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebFilter("/*")
public class AuthFilter implements Filter {
//    private static final List<String> methodsNeedCSRF = List.of("POST", "PUT", "DELETE");
    private static final List<String> allowedPaths = List.of("/api/auth", "/ws/score");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = req.getRequestURI();
        httpResponse.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, JSESSIONID, X-CSRF-TOKEN");
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
        httpResponse.setHeader("Content-Type", "application/json");
        if (allowedPaths.stream().anyMatch(path::startsWith)) {
            chain.doFilter(request, response);
            return;
        }
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.getAttributeNames().asIterator().forEachRemaining((String name) -> {
                System.out.println(name + " : " + session.getAttribute(name));
            });
        }
        if (session != null && session.getAttribute("uid") != null) {
            String agentFromSession = (String) session.getAttribute("agent");
            String agentFromHeader = req.getHeader("User-Agent");

            if (agentFromSession == null || !agentFromSession.equals(agentFromHeader)) {
                session.invalidate();
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("CSRF token validation failed");
                return;
            } else if (!"GET".equalsIgnoreCase(req.getMethod())) {
                String csrfTokenFromSession = (String) session.getAttribute("csrfToken");
                String csrfTokenFromHeader = req.getHeader("X-CSRF-TOKEN");
                System.out.println("CSRF token from session: " + csrfTokenFromHeader);
                if (csrfTokenFromSession == null || !csrfTokenFromSession.equals(csrfTokenFromHeader)) {
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("CSRF token validation failed");
                    return;
                }
            }
            chain.doFilter(request, response);
        } else {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("No valid session found");
        }
    }
}