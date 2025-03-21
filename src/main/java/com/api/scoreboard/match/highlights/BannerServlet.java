package com.api.scoreboard.match.highlights;

import com.api.util.Database;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/image/banner")
public class BannerServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        // get match id from request parameter
        String matchId = request.getParameter("id");
        if (matchId == null || matchId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // get banner image from database
        // send image to response
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = new Database().getConnection();
            stmt = conn.prepareStatement("SELECT banner_path FROM matches WHERE id = ?");
            stmt.setString(1, matchId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                String bannerPath = rs.getString("banner_path");
                if (bannerPath == null || bannerPath.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                response.setContentType("image/jpeg");
                String path = "F:\\Code\\JAVA\\zoho_training\\uploads\\banners\\" + bannerPath;
                File imageFile = new File(path);
                if (!imageFile.exists()) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                sendImageFile(response, imageFile);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            System.out.println("Error fetching banner image: " + e.getMessage());
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
            } catch (SQLException e) {
                System.out.println("Error closing resource: " + e.getMessage());
            }
        }
    }

    private void sendImageFile(HttpServletResponse response, File imageFile) throws IOException {
        response.setContentLengthLong(imageFile.length());
        response.setContentType("image/jpeg");

        try {
            FileInputStream fis = new FileInputStream(imageFile);
            OutputStream os = response.getOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                os.flush();
            }
        } catch (IOException e) {
            System.out.println("Error sending video: " + e.getMessage());
        }
    }
}
