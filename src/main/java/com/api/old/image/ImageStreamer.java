package com.api.old.image;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.*;
import java.nio.ByteBuffer;

@ServerEndpoint("/imagews")
public class ImageStreamer {
    private final static String imagePathSmall = "C:\\Users\\ACER\\Downloads\\testimage\\small.jpg";
    private final static String imagePathMedium = "C:\\Users\\ACER\\Downloads\\testimage\\medium.jpg";
    private final static String imagePathBig = "C:\\Users\\ACER\\Downloads\\testimage\\large.png";

    private final static int KILOBYTE = 1024;
    private final static int MEGABYTE = 1024 * KILOBYTE;

    private long startTime;
    private final File imageFile = new File(imagePathMedium);

    private BufferedInputStream bufferedStream;
    private byte[] buffer;
    private int chunkSize;
    private int bytesRead;

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Open session: " + session.getId());
        session.setMaxIdleTimeout(10 * 60 * 1000);
        session.setMaxBinaryMessageBufferSize(1024 * 1024);
        RemoteEndpoint.Basic remote = session.getBasicRemote();

        try {
            InputStream imageStream = new FileInputStream(imageFile);
            bufferedStream = new BufferedInputStream(imageStream);
            chunkSize = MEGABYTE / 2;
            buffer = new byte[chunkSize];
            startTime = System.currentTimeMillis();

            if ((bytesRead = bufferedStream.read(buffer)) != -1) {
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
                try {
                    remote.sendBinary(byteBuffer);
                } catch (Exception e) {
                    System.out.println("error" + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("error" + e.getMessage());
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        long endTime = System.currentTimeMillis();
        double diffSecs = (endTime - startTime) / 1000.0;
        System.out.println(diffSecs + " seconds" + " size" + (chunkSize / diffSecs));
        chunkSize = (int) (chunkSize / diffSecs);
        if (chunkSize > 10 * MEGABYTE) {
            chunkSize = 10 * MEGABYTE;
        }
        buffer = new byte[chunkSize];
        startTime = System.currentTimeMillis();
        try {
            if ((bytesRead = bufferedStream.read(buffer)) != -1) {
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
                try {
                    session.getBasicRemote().sendBinary(byteBuffer);
                } catch (Exception e) {
                    System.out.println("error" + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("error" + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Closed session: " + session.getId());
    }
}