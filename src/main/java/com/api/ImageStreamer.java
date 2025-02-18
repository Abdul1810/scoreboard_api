package com.api;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

@ServerEndpoint("/imagews")
public class ImageStreamer {
    private final static String imagePathSmall = "C:\\Users\\ACER\\Downloads\\testimage\\small.jpg";
    private final static String imagePathMedium = "C:\\Users\\ACER\\Downloads\\testimage\\medium.jpg";
    private final static String imagePathBig = "C:\\Users\\ACER\\Downloads\\testimage\\large.png";

    private final static int KILOBYTE = 1024;
    private final static int MEGABYTE = 1024 * KILOBYTE;

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Opened session: " + session.getId());
        session.setMaxIdleTimeout(10 * 60 * 1000);
        session.setMaxBinaryMessageBufferSize(1024 * 1024);

        RemoteEndpoint.Async remote = session.getAsyncRemote();
        remote.sendText("starting");
        File imageFile = new File(imagePathMedium);
        double fileSize = imageFile.length();

        try (InputStream imageStream = new FileInputStream(imageFile)) {
            BufferedInputStream bufferedStream = new BufferedInputStream(imageStream);
            int chunkSize = 1024 * KILOBYTE;
//            if (fileSize <= 96 * KILOBYTE) {
//                chunkSize = 8 * KILOBYTE;
//            } else if (fileSize <= 384 * KILOBYTE) {
//                chunkSize = 32 * KILOBYTE;
//            } else if (fileSize <= 1536 * KILOBYTE) {
//                chunkSize = 128 * KILOBYTE;
//            } else if (fileSize <= 5120 * KILOBYTE) {
//                chunkSize = 512 * KILOBYTE;
//            } else if (fileSize <= 24576 * KILOBYTE) {
//                chunkSize = 2048 * KILOBYTE;
//            } else if (fileSize <= 102400 * KILOBYTE) {
//                chunkSize = 5120 * KILOBYTE;
//            } else {
//                chunkSize = 10240 * KILOBYTE;
//            }
            byte[] buffer = new byte[chunkSize];
            int bytesRead;
            int count = 0;

            while ((bytesRead = bufferedStream.read(buffer)) != -1) {
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
                long startTime = System.nanoTime();
                try {remote.sendBinary(byteBuffer);
                } catch (Exception e) {
                    System.out.println("Error sending chunk " + count + ": " + e);
                }
                long endTime = System.nanoTime();
                long timeTaken = endTime - startTime;
                double diffSeconds = timeTaken / 1_000_000_000.0;
                System.out.println("Time taken to send chunk " + count + ": " + diffSeconds + " s");
                chunkSize = (int) (chunkSize / diffSeconds);
                System.out.println("New chunk size: " + chunkSize);
                if (chunkSize > 5 * MEGABYTE) {
                    chunkSize = 5 * MEGABYTE;
                } else if (chunkSize < MEGABYTE) {
                    chunkSize = MEGABYTE;
                }
                buffer = new byte[chunkSize];
                count++;
            }
            System.out.println("image send  done" + count);
        } catch (Exception e) {
            System.out.println("Streaming error: " + e);
        }
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Closed session: " + session.getId());
    }
}
