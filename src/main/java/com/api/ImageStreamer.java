package com.api;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ServerEndpoint("/imagews")
public class ImageStreamer {
    private final static String imagePathSmall = "C:\\Users\\ACER\\Downloads\\testimage\\small.jpg";
    private final static String imagePathMedium = "C:\\Users\\ACER\\Downloads\\testimage\\medium.jpg";
    private final static String imagePathBig = "C:\\Users\\ACER\\Downloads\\testimage\\large.png";

    private final static int KILOBYTE = 1024;
    private final static int MEGABYTE = 1024 * KILOBYTE;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private CompletableFuture<Void> future;

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Open session: " + session.getId());
        session.setMaxIdleTimeout(10 * 60 * 1000);
        session.setMaxBinaryMessageBufferSize(1024 * 1024);

        RemoteEndpoint.Basic remote = session.getBasicRemote();
        executor.submit(() -> {
            File imageFile = new File(imagePathMedium);
            try (InputStream imageStream = new FileInputStream(imageFile)) {
                BufferedInputStream bufferedStream = new BufferedInputStream(imageStream);
                int chunkSize = MEGABYTE;

                byte[] buffer = new byte[chunkSize];
                int bytesRead;
                int count = 0;

                while ((bytesRead = bufferedStream.read(buffer)) != -1) {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
                    long startTime = System.currentTimeMillis();
                    try {
                        remote.sendBinary(byteBuffer);
                    } catch (Exception e) {
                        System.out.println("Error sending chunk " + count + ": " + e);
                    }
                    future = new CompletableFuture<>();
                    try {
                        future.get();
                    } catch (Exception e) {
                        System.out.println("Error waiting for message: " + e);
                    }
                    long endTime = System.currentTimeMillis();
                    double diffSecs = (endTime - startTime) / 1000.0;
                    System.out.println("Chunk " + count + " sent in " + diffSecs + " seconds");
                    chunkSize = (int) (chunkSize / diffSecs);
//                    if (chunkSize < 8 * KILOBYTE) {
//                        chunkSize = 8 * KILOBYTE;
//                    } else if (chunkSize > 10 * MEGABYTE) {
//                        chunkSize = 10 * MEGABYTE;
//                    }
//                    chunkSize=4 * KILOBYTE;
                    buffer = new byte[chunkSize];
                    count++;
                }
                System.out.println("image send  done" + count);
            } catch (Exception e) {
                System.out.println("Streaming error: " + e);
            }
        });
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("Received message: " + message);
        if (future != null) {
            future.complete(null);
        }
    }

    @OnClose
    public void onClose(Session session) {
        executor.shutdown();
        System.out.println("Closed session: " + session.getId());
    }
}