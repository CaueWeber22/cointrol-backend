package com.fcproject.adapters.outbound.persistence;

import com.fcproject.adapters.outbound.entities.auth.SecurityAuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SecurityAuditEventJPARepository extends JpaRepository<SecurityAuditEventEntity, UUID> {
}
