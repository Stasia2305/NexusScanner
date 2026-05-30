package com.nexusscan.service.strategy;

import com.nexusscan.service.ScanService;

/**
 * Interface for the Strategy Pattern to define different document splitting algorithms.
 */
public interface ISplitStrategy {
    boolean shouldSplit(ScanService.ScanResult result, int totalScans, String splitLogic);
}
