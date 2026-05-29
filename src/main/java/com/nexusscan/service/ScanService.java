package com.nexusscan.service;

import java.util.Random;

/**
 * Service simulating a hardware scanner.
 * In a production environment, this would interface with TWAIN or WIA drivers.
 */
public class ScanService {
    private static ScanService instance;
    private Random random = new Random();

    /**
     * Represents the result of a single scan operation.
     */
    public static class ScanResult {
        private String imagePath;
        private boolean isBarcode;
        private byte[] data;

        public ScanResult(String imagePath, boolean isBarcode, byte[] data) {
            this.imagePath = imagePath;
            this.isBarcode = isBarcode;
            this.data = data;
        }

        public String getImagePath() { return imagePath; }
        public boolean isBarcode() { return isBarcode; }
        public byte[] getData() { return data; }
    }

    private ScanService() {}

    public static ScanService getInstance() {
        if (instance == null) {
            instance = new ScanService();
        }
        return instance;
    }

    /**
     * Simulates a scan. Has a 10% chance to return a barcode document.
     * Otherwise returns a placeholder image from a public API.
     */
    public ScanResult scan() {
        boolean isBarcode = random.nextInt(10) == 0; // 10% chance
        String id = String.valueOf(random.nextInt(100000));
        
        if (isBarcode) {
            return new ScanResult("BARCODE-" + id, true, null);
        } else {
            // Simulate random image path/data
            return new ScanResult("https://picsum.photos/seed/" + id + "/800/600", false, new byte[0]);
        }
    }
}
