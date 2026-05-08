package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.NotificationDispatchEvent;
import com.jobportal.notificationservice.entity.Notification;
import com.jobportal.notificationservice.entity.NotificationPreference;
import com.jobportal.notificationservice.entity.NotificationStatus;
import com.jobportal.notificationservice.exception.NotificationNotFoundException;
import com.jobportal.notificationservice.repository.NotificationRepository;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static com.jobportal.notificationservice.config.NotificationTopologyProperties.NOTIFICATION_DISPATCH_QUEUE;

@Service
public class NotificationDispatchConsumer {

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryService notificationDeliveryService;
    private final NotificationPreferenceService notificationPreferenceService;

    public NotificationDispatchConsumer(
            NotificationRepository notificationRepository,
            NotificationDeliveryService notificationDeliveryService,
            NotificationPreferenceService notificationPreferenceService
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationDeliveryService = notificationDeliveryService;
        this.notificationPreferenceService = notificationPreferenceService;
    }

    @Transactional
    @RabbitListener(queues = NOTIFICATION_DISPATCH_QUEUE, ackMode = "MANUAL")
    public void onNotificationDispatch(NotificationDispatchEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        Notification notification = notificationRepository.findById(event.notificationId())
                .orElseThrow(() -> new NotificationNotFoundException(event.notificationId()));

        if (notification.getStatus() == NotificationStatus.SENT) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        NotificationPreference preference = notificationPreferenceService.resolvePreference(
                notification.getRecipientUserId(),
                notification.getEventType()
        );
        notificationDeliveryService.continueEmailDelivery(notification, preference);

        if (notification.getStatus() == NotificationStatus.RETRYING) {
            channel.basicNack(deliveryTag, false, true);
            return;
        }

        channel.basicAck(deliveryTag, false);
    }
}
