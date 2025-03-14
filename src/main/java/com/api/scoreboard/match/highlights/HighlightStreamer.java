package com.api.scoreboard.match.highlights;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@WebServlet("/video/highlights")
public class HighlightStreamer extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        String videoName = request.getParameter("name");
        if (videoName == null || videoName.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        File videoFile = new File("F:\\Code\\JAVA\\zoho_training\\uploads\\highlights\\" + videoName);
        if (!videoFile.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            sendVideoFile(response, videoFile);
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void sendVideoFile(HttpServletResponse response, File videoFile) throws IOException {
        response.setContentLengthLong(videoFile.length());
        response.setHeader("Accept-Ranges", "bytes");
        response.setContentType("video/mp4");

        try {
            FileInputStream fis = new FileInputStream(videoFile);
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