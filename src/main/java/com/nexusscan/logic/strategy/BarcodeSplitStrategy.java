package com.nexusscan.logic.strategy;

import com.nexusscan.logic.ScanService;

/**
 * Concrete strategy that triggers a split when a barcode is detected.
 */
public class BarcodeSplitStrategy implements ISplitStrategy {
    @Override
    public boolean shouldSplit(ScanService.ScanResult result, int totalScans, String splitLogic) {
        return result.isBarcode();
    }
}
