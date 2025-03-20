package com.api.scoreboard.embed;

import com.api.util.Database;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@ServerEndpoint("/ws/score")
public class EmbedSocket {
    @OnOpen
    public void onOpen(Session session) {
        session.setMaxIdleTimeout(60 * 60 * 1000);
        session.setMaxTextMessageBufferSize(1024 * 1024);
        session.setMaxBinaryMessageBufferSize(10 * 1024 * 1024);

        System.out.println("Open session" + session.getId());
        String embedCode = session.getRequestParameterMap().get("verificationCode").get(0);
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = new Database().getConnection();
            stmt = conn.prepareStatement("SELECT * FROM embeds WHERE embed_code = ?");
            stmt.setString(1, embedCode);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                session.close();
            } else {
                String matchId = rs.getString("match_id");
                System.out.println("Match ID: " + matchId);
                EmbedListener.addSession(embedCode, session, matchId);
            }
        } catch (Exception e) {
            System.out.println("Error while searching embed code in the database : " + e.getMessage());
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
                System.out.println("Error closing resource : " + e.getMessage());
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Close session" + session.getId());
        String embedCode = session.getRequestParameterMap().get("verificationCode").get(0);
        EmbedListener.removeSession(embedCode, session);
    }
}
