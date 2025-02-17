package com.api;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.*;

@WebServlet("/image")
public class ImageStreamer extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String imagePath = "C:\\Users\\ACER\\Downloads\\52259221868_3d2963c1fe_o.png";
        File imageFile = new File(imagePath);

        response.setContentType("image/png");
        response.setContentLengthLong(imageFile.length());

        try (BufferedInputStream imageIn = new BufferedInputStream(new FileInputStream(imageFile)); OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = imageIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
        }
    }
}
