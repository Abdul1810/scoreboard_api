package com.api.scoreboard.match;


import jakarta.servlet.http.HttpSession;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

public class UidConfigurator extends ServerEndpointConfig.Configurator {
    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        Object sessionObj = request.getHttpSession();
        if (sessionObj instanceof HttpSession) {
            HttpSession httpSession = (HttpSession) sessionObj;
            Object userId = httpSession.getAttribute("uid");
            if (userId != null) {
                config.getUserProperties().put("uid", userId);
                System.out.println("UserId set in WebSocket session: " + userId);
            } else {
                System.out.println("No userId found in HttpSession");
            }
        } else {
            System.out.println("HttpSession is null or invalid");
        }
    }
}
