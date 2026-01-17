package com.agriprocurement.procurement.infrastructure;

import com.agriprocurement.procurement.application.ProcurementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcurementScheduledTasks {

    private final ProcurementService procurementService;

    @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
    public void closeExpiredBidding() {
        log.info("Running scheduled task: closeExpiredBidding");
        try {
            procurementService.closeExpiredBidding();
        } catch (Exception e) {
            log.error("Error in scheduled task closeExpiredBidding", e);
        }
    }
}
