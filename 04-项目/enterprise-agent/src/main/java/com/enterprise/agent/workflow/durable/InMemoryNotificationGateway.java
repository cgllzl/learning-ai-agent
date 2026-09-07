package com.enterprise.agent.workflow.durable;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryNotificationGateway implements NotificationGateway {

    private final List<String> sentMessages = new CopyOnWriteArrayList<>();

    @Override
    public String sendStatusChanged(String userId, String orderId, String newStatus) {
        String message = "已通知用户 " + userId + "：订单 " + orderId + " 状态变为 " + newStatus;
        sentMessages.add(message);
        return message;
    }

    public List<String> sentMessages() {
        return List.copyOf(sentMessages);
    }
}
