package com.nexusscan.logic.strategy;

import com.nexusscan.logic.ScanService;

/**
 * Concrete strategy that triggers a split when a barcode is detected.
 */
public class BarcodeSplitStrategy implements ISplitStrategy {
    /**
     * Determines whether a split should occur, triggering if the scan result detected a barcode.
     *
     * @param result     The scan result of the current page.
     * @param totalScans The total number of scans.
     * @param splitLogic The split logic rule or setting.
     * @return True if the result contains a barcode, which triggers a new document split.
     */
    @Override
    public boolean shouldSplit(ScanService.ScanResult result, int totalScans, String splitLogic) {
        return result.isBarcode();
    }
}
