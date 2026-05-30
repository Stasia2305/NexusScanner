package com.nexusscan.service.strategy;

import com.nexusscan.service.ScanService;

/**
 * Concrete strategy that triggers a split after a fixed number of pages.
 */
public class FixedPageSplitStrategy implements ISplitStrategy {
    @Override
    public boolean shouldSplit(ScanService.ScanResult result, int totalScans, String splitLogic) {
        if (splitLogic != null && !splitLogic.isEmpty()) {
            try {
                int interval = Integer.parseInt(splitLogic.trim());
                return interval > 0 && totalScans > 0 && totalScans % interval == 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}
