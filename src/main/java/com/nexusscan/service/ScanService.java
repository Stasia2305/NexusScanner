package com.nexusscan.service;

import java.util.Random;

/**
 * The ScanService simulates a real paper scanner.
 * In a real-world scenario, this would talk to actual scanner hardware (using TWAIN or WIA).
 */
public class ScanService {
    private static ScanService instance; // The single copy of ScanService used by the whole app
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
     * Simulates scanning a single sheet of paper.
     * It has a small chance of "finding" a barcode, which triggers a document split.
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
