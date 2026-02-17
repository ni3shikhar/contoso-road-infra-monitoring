package com.contoso.roadinfra.alert.service;

import com.contoso.roadinfra.alert.constants.AlertStatus;
import com.contoso.roadinfra.alert.entity.Alert;
import com.contoso.roadinfra.alert.entity.AlertRule;
import com.contoso.roadinfra.alert.mapper.AlertMapper;
import com.contoso.roadinfra.alert.repository.AlertRepository;
import com.contoso.roadinfra.alert.repository.AlertRuleRepository;
import com.contoso.roadinfra.common.constants.AlertSeverity;
import com.contoso.roadinfra.common.dto.AlertDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling alert escalation logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EscalationService {

    private final AlertRepository alertRepository;
    private final AlertRuleRepository ruleRepository;
    private final AlertMapper alertMapper;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TaskScheduler taskScheduler;

    /**
     * Schedule an alert for escalation after specified minutes.
     */
    public void scheduleEscalation(UUID alertId, int minutes) {
        taskScheduler.schedule(
                () -> escalateAlert(alertId),
                Instant.now().plusSeconds(minutes * 60L)
        );
        log.debug("Scheduled escalation for alert {} in {} minutes", alertId, minutes);
    }

    /**
     * Escalate a specific alert.
     */
    public void escalateAlert(UUID alertId) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            // Only escalate if still active
            if (alert.getAlertStatus().isActive() && !alert.getResolved()) {
                performEscalation(alert);
            }
        });
    }

    /**
     * Process periodic escalation check for all active alerts.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void processEscalations() {
        log.debug("Processing scheduled escalations");
        
        // Find alerts that have been open for more than 30 minutes without escalation
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        List<Alert> alertsToEscalate = alertRepository.findAlertsNeedingEscalation(cutoff);

        for (Alert alert : alertsToEscalate) {
            // Only escalate if rule has escalation configured
            if (alert.getRuleId() != null) {
                ruleRepository.findById(alert.getRuleId()).ifPresent(rule -> {
                    if (rule.getEscalationMinutes() != null && 
                            shouldEscalate(alert, rule.getEscalationMinutes())) {
                        performEscalation(alert);
                    }
                });
            } else {
                // Default escalation for alerts without rules
                if (shouldDefaultEscalate(alert)) {
                    performEscalation(alert);
                }
            }
        }
    }

    private boolean shouldEscalate(Alert alert, int escalationMinutes) {
        LocalDateTime escalationThreshold = alert.getTriggeredAt().plusMinutes(
                (long) escalationMinutes * (alert.getEscalationLevel() + 1)
        );
        return LocalDateTime.now().isAfter(escalationThreshold);
    }

    private boolean shouldDefaultEscalate(Alert alert) {
        // Default: escalate after 60 minutes for level 0, 120 for level 1, etc.
        int minutesThreshold = 60 * (alert.getEscalationLevel() + 1);
        LocalDateTime escalationThreshold = alert.getTriggeredAt().plusMinutes(minutesThreshold);
        return LocalDateTime.now().isAfter(escalationThreshold);
    }

    private void performEscalation(Alert alert) {
        if (alert.getEscalationLevel() >= 3) {
            log.warn("Alert {} has reached maximum escalation level", alert.getId());
            return;
        }

        AlertSeverity newSeverity = escalateSeverity(alert.getSeverity());
        int newLevel = alert.getEscalationLevel() + 1;

        alert.setSeverity(newSeverity);
        alert.setEscalationLevel(newLevel);
        alert.setAlertStatus(AlertStatus.ESCALATED);
        alert.setStatus("ESCALATED");
        alert.setEscalatedAt(LocalDateTime.now());

        Alert saved = alertRepository.save(alert);
        log.info("Escalated alert {}: level {} -> {}, severity {} -> {}", 
                alert.getId(), newLevel - 1, newLevel, alert.getOriginalSeverity(), newSeverity);

        AlertDTO dto = alertMapper.toDto(saved);
        
        // Broadcast update
        messagingTemplate.convertAndSend("/topic/alerts", dto);
        kafkaTemplate.send("alert-events", "alert.escalated", dto);
        
        // Send escalation notification
        notificationService.sendEscalationNotification(dto, newLevel);
    }

    private AlertSeverity escalateSeverity(AlertSeverity current) {
        return switch (current) {
            case INFO -> AlertSeverity.LOW;
            case LOW -> AlertSeverity.MEDIUM;
            case MEDIUM -> AlertSeverity.HIGH;
            case HIGH, CRITICAL -> AlertSeverity.CRITICAL;
        };
    }

    /**
     * Get escalation statistics.
     */
    @Transactional(readOnly = true)
    public EscalationStats getEscalationStats() {
        List<Alert> activeAlerts = alertRepository.findActiveAlerts();
        
        long escalatedCount = activeAlerts.stream()
                .filter(a -> a.getEscalationLevel() > 0)
                .count();
        
        long criticalEscalated = activeAlerts.stream()
                .filter(a -> a.getEscalationLevel() > 0 && a.getSeverity() == AlertSeverity.CRITICAL)
                .count();
        
        return new EscalationStats(
                activeAlerts.size(),
                escalatedCount,
                criticalEscalated
        );
    }

    public record EscalationStats(long totalActive, long escalated, long criticalEscalated) {}
}
