package com.fcproject.adapters.outbound;

import com.fcproject.adapters.outbound.entities.auth.SecurityAuditEventEntity;
import com.fcproject.adapters.outbound.persistence.SecurityAuditEventJPARepository;
import com.fcproject.application.core.domain.auth.SecurityAuditEvent;
import com.fcproject.application.ports.outbound.SecurityAuditOutPort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class SecurityAuditPersistenceAdapter implements SecurityAuditOutPort {
    private final SecurityAuditEventJPARepository events;

    public SecurityAuditPersistenceAdapter(SecurityAuditEventJPARepository events) {
        this.events = events;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(SecurityAuditEvent event) {
        events.save(new SecurityAuditEventEntity(
                event.id(),
                event.userId(),
                event.identifierHash(),
                event.eventType(),
                event.clientIp(),
                event.userAgent(),
                event.occurredAt()
        ));
    }
}
