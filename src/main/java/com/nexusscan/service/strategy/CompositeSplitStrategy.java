package com.nexusscan.service.strategy;

import com.nexusscan.service.ScanService;
import java.util.ArrayList;
import java.util.List;

/**
 * Composite strategy that allows multiple splitting rules to be active at once.
 */
public class CompositeSplitStrategy implements ISplitStrategy {
    private final List<ISplitStrategy> strategies = new ArrayList<>();

    public void addStrategy(ISplitStrategy strategy) {
        strategies.add(strategy);
    }

    @Override
    public boolean shouldSplit(ScanService.ScanResult result, int totalScans, String splitLogic) {
        for (ISplitStrategy strategy : strategies) {
            if (strategy.shouldSplit(result, totalScans, splitLogic)) {
                return true;
            }
        }
        return false;
    }
}
