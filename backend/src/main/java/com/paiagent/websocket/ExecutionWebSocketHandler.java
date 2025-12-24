package com.paiagent.websocket;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流执行 WebSocket 处理器
 */
@Slf4j
@Component
public class ExecutionWebSocketHandler extends TextWebSocketHandler {

    // 存储所有连接的 WebSocket 会话
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("WebSocket 连接建立: {}", sessionId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        log.info("WebSocket 连接关闭: {}", sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 处理客户端发送的消息（如果需要）
        String payload = message.getPayload();
        log.info("收到 WebSocket 消息: {}", payload);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 传输错误: {}", session.getId(), exception);
        if (session.isOpen()) {
            session.close();
        }
        sessions.remove(session.getId());
    }

    /**
     * 广播消息到所有连接的客户端
     */
    public void broadcast(Object message) {
        String jsonMessage = JSON.toJSONString(message);
        TextMessage textMessage = new TextMessage(jsonMessage);

        sessions.forEach((id, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("发送 WebSocket 消息失败: {}", id, e);
                }
            }
        });

        log.debug("广播消息到 {} 个客户端: {}", sessions.size(), jsonMessage);
    }

    /**
     * 发送消息到指定会话
     */
    public void sendToSession(String sessionId, Object message) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = JSON.toJSONString(message);
                session.sendMessage(new TextMessage(jsonMessage));
                log.debug("发送消息到会话 {}: {}", sessionId, jsonMessage);
            } catch (IOException e) {
                log.error("发送消息到会话失败: {}", sessionId, e);
            }
        }
    }

    /**
     * 获取在线连接数
     */
    public int getOnlineCount() {
        return sessions.size();
    }
}
