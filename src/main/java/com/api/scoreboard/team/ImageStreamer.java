package com.api.scoreboard.team;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@WebServlet("/image/*")
public class ImageStreamer extends HttpServlet {

    private static final List<String> allowedTypes = List.of("teams", "players");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        String imageName = request.getParameter("name");
        String qualityParam = request.getParameter("q");

        String pathInfo = request.getPathInfo();
        if (imageName == null || imageName.isEmpty() || pathInfo == null || pathInfo.length() <= 1) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String type = pathInfo.substring(1);
        if (!allowedTypes.contains(type)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String imagePath = "F:\\Code\\JAVA\\zoho_training\\uploads\\" + type + "\\" + imageName;
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            BufferedImage originalImage = ImageIO.read(imageFile);
            if (originalImage == null) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            BufferedImage resizedImage;
            switch (qualityParam.toLowerCase()) {
                case "low":
                    resizedImage = resizeImage(originalImage, 64, 64);
                    break;
                case "medium":
                    resizedImage = resizeImage(originalImage, 128, 128);
                    break;
                case "high":
                    resizedImage = originalImage;
                    break;
                default:
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
            }

            response.setContentType("image/jpeg");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();

            try (OutputStream os = response.getOutputStream()) {
                int offset = 0;
                while (offset < imageBytes.length) {
                    int chunkSize = Math.min(8192, imageBytes.length - offset);
                    os.write(imageBytes, offset, chunkSize);
                    os.flush();
                    offset += chunkSize;
                }
            }

        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            System.out.println("Error fetching image: " + e.getMessage());
        }
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        double scale = Math.min((double) targetWidth / originalWidth, (double) targetHeight / originalHeight);
        int scaledWidth = (int) (originalWidth * scale);
        int scaledHeight = (int) (originalHeight * scale);

        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        return scaledImage;
    }
}