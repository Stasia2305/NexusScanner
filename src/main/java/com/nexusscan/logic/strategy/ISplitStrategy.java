package com.nexusscan.logic.strategy;

import com.nexusscan.logic.ScanService;

/**
 * Interface for the Strategy Pattern to define different document splitting algorithms.
 */
public interface ISplitStrategy {
    boolean shouldSplit(ScanService.ScanResult result, int totalScans, String splitLogic);
}
