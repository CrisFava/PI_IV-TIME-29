// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.ride.infrastructure.listener;

import com.converge.api.modules.ride.application.event.RideNotificationEvent;
import com.converge.api.shared.application.AuditLogService;
import com.converge.api.shared.domain.enums.AuditAction;
import com.converge.api.shared.domain.enums.NotificationType;
import com.converge.api.shared.domain.model.AuditLog;
import com.converge.api.shared.domain.model.Notification;
import com.converge.api.shared.domain.repository.AuditLogRepository;
import com.converge.api.shared.domain.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Testes unitários para gravação de trilha de auditoria e listener de notificações de viagem.
 */
@ExtendWith(MockitoExtension.class)
class AuditAndNotificationListenerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("Deve gravar registro imutável na trilha de auditoria (audit_logs)")
    void shouldRecordAuditLog() {
        AuditLogService auditLogService = new AuditLogService(auditLogRepository);

        auditLogService.record(
                AuditAction.RIDE_CANCELLED,
                "driver-123",
                "passenger-456",
                "RIDE",
                "ride-789",
                Map.of("reason", "Imprevisto")
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(AuditAction.RIDE_CANCELLED);
        assertThat(saved.getActorId()).isEqualTo("driver-123");
        assertThat(saved.getTargetUserId()).isEqualTo("passenger-456");
        assertThat(saved.getEntityType()).isEqualTo("RIDE");
        assertThat(saved.getEntityId()).isEqualTo("ride-789");
        assertThat(saved.getMetadata()).containsEntry("reason", "Imprevisto");
    }

    @Test
    @DisplayName("Deve persistir notificações para todos os passageiros confirmados ao receber RideNotificationEvent")
    void shouldPersistNotificationsForAllRecipientUsers() {
        RideNotificationListener listener = new RideNotificationListener(notificationRepository);
        RideNotificationEvent event = new RideNotificationEvent(
                List.of("passenger-1", "passenger-2"),
                NotificationType.RIDE_CANCELLED,
                "Viagem cancelada",
                "O motorista cancelou a viagem.",
                "ride-789"
        );

        listener.onRideNotification(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());

        List<Notification> notifications = captor.getValue();
        assertThat(notifications).hasSize(2);
        assertThat(notifications.get(0).getUserId()).isEqualTo("passenger-1");
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.RIDE_CANCELLED);
        assertThat(notifications.get(0).getRelatedEntityId()).isEqualTo("ride-789");
        assertThat(notifications.get(0).isRead()).isFalse();
    }
}
