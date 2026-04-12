package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.event.ApplicationStatusUpdatedEvent;
import com.jobportal.applicationservice.event.ApplicationSubmittedEvent;
import com.jobportal.applicationservice.event.ApplicationWithdrawnEvent;

public interface ApplicationEventPublisher {

    void publishSubmitted(ApplicationSubmittedEvent event);

    void publishStatusUpdated(ApplicationStatusUpdatedEvent event);

    void publishWithdrawn(ApplicationWithdrawnEvent event);
}
