package com.api.scoreboard.stats;

import com.api.util.Database;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebFilter("/ws/stats")
public class StatsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.getAttributeNames().asIterator().forEachRemaining((String name) -> {
                System.out.println(name + " : " + session.getAttribute(name));
            });
            int userId = (int) session.getAttribute("uid");
            httpRequest.setAttribute("uid", userId);
            int matchId = Integer.parseInt(httpRequest.getParameter("id"));

            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                conn = new Database().getConnection();
                stmt = conn.prepareStatement("SELECT * FROM matches WHERE id = ? AND user_id = ?");
                stmt.setInt(1, matchId);
                stmt.setInt(2, userId);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    chain.doFilter(request, response);
                } else {
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("You are not authorized to view this match");
                }
            } catch (Exception e) {
                httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Internal server error");
                System.out.println("Error while fetching match data : " + e.getMessage());
            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (stmt != null) {
                        stmt.close();
                    }
                    if (conn != null) {
                        conn.close();
                    }
                } catch (SQLException ex) {
                    System.out.println("Error closing resource: " + ex.getMessage());
                }
            }
        } else {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("No valid session found");
        }
    }
}