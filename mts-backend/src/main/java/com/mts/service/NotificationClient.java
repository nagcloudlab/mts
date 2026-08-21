package com.mts.service;

import com.mts.dto.TransferResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestTemplate restTemplate;
    private final String notificationServiceUrl;

    public NotificationClient(
            RestTemplate restTemplate,
            @Value("${mts.notification-service.url}") String notificationServiceUrl) {
        this.restTemplate = restTemplate;
        this.notificationServiceUrl = notificationServiceUrl;
    }

    public void sendTransactionNotification(TransferResponse response) {
        try {
            restTemplate.postForEntity(
                    notificationServiceUrl + "/api/notifications/transaction",
                    response,
                    Void.class
            );
            log.info("Notification sent for transaction: {}", response.getReferenceId());
        } catch (Exception e) {
            // Non-blocking: log and continue even if notification fails
            log.warn("Failed to send notification for transaction {}: {}",
                    response.getReferenceId(), e.getMessage());
        }
    }
}
