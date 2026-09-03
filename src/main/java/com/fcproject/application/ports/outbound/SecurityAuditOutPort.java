package com.fcproject.application.ports.outbound;

import com.fcproject.application.core.domain.auth.SecurityAuditEvent;

public interface SecurityAuditOutPort {
    void record(SecurityAuditEvent event);
}
