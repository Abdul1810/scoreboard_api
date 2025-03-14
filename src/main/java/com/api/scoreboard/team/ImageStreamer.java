package com.api.scoreboard.team;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@WebServlet("/image")
public class ImageStreamer extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        String imageName = request.getParameter("name");
        String type = request.getParameter("type");
        if (imageName == null || imageName.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (type == null || type.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String path = "";
        if (type.equals("team")) {
            path = "F:\\Code\\JAVA\\zoho_training\\uploads\\teams\\" + imageName;
        } else if (type.equals("player")) {
            path = "F:\\Code\\JAVA\\zoho_training\\uploads\\players\\" + imageName;
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        File imageFile = new File(path);
        if (!imageFile.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            sendImageFile(response, imageFile);
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
            System.out.println("Error sending imageFile: " + e.getMessage());
        }
    }
}