package com.nexusscan.logic.strategy;

import com.nexusscan.logic.ScanService;

/**
 * Interface for the Strategy Pattern to define different document splitting algorithms.
 */
public interface ISplitStrategy {
    /**
     * Determines whether a new document split should occur based on the scan result.
     *
     * @param result     The scan result of the current page.
     * @param totalScans The total number of scans in the current session.
     * @param splitLogic The split logic rule or setting.
     * @return True if a new document should be started, false otherwise.
     */
    boolean shouldSplit(ScanService.ScanResult result, int totalScans, String splitLogic);
}
