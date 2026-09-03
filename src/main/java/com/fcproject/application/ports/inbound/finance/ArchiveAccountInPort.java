package com.fcproject.application.ports.inbound.finance;

import java.util.UUID;

public interface ArchiveAccountInPort {
    void archiveAccount(UUID userId, UUID accountId);
}
