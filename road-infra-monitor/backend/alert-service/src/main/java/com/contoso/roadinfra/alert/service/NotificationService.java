package com.contoso.roadinfra.alert.service;

import com.contoso.roadinfra.common.dto.AlertDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    @Async
    public void sendNotifications(AlertDTO alert) {
        log.info("Sending notifications for alert: {}", alert.getTitle());

        // Email notification
        sendEmailNotification(alert);

        // SMS notification for critical alerts
        if ("Critical".equals(alert.getSeverity().getDisplayName())) {
            sendSmsNotification(alert);
        }

        // Webhook notification
        sendWebhookNotification(alert);

        log.info("Notifications sent for alert: {}", alert.getId());
    }

    private void sendEmailNotification(AlertDTO alert) {
        log.debug("Sending email notification for alert: {}", alert.getId());
        // Email implementation would go here
    }

    private void sendSmsNotification(AlertDTO alert) {
        log.debug("Sending SMS notification for alert: {}", alert.getId());
        // SMS implementation would go here
    }

    private void sendWebhookNotification(AlertDTO alert) {
        log.debug("Sending webhook notification for alert: {}", alert.getId());
        // Webhook implementation would go here
    }

    /**
     * Send escalation notification.
     */
    @Async
    public void sendEscalationNotification(AlertDTO alert, int escalationLevel) {
        log.info("Sending escalation notification for alert {} (level {})", alert.getId(), escalationLevel);
        
        // Always send email for escalations
        sendEmailNotification(alert);
        
        // Send SMS for level 2+ escalations
        if (escalationLevel >= 2) {
            sendSmsNotification(alert);
        }
        
        // Send to all channels for level 3 (maximum) escalations
        if (escalationLevel >= 3) {
            sendWebhookNotification(alert);
            log.warn("CRITICAL ESCALATION: Alert {} has reached maximum escalation level!", alert.getId());
        }
        
        log.info("Escalation notifications sent for alert {} at level {}", alert.getId(), escalationLevel);
    }
}
