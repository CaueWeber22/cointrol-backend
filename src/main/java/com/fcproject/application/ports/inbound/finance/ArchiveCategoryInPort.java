package com.fcproject.application.ports.inbound.finance;

import java.util.UUID;

public interface ArchiveCategoryInPort {
    void archiveCategory(UUID userId, UUID categoryId);
}
