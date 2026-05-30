package com.nexusscan.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The ScanService simulates a real paper scanner.
 * It now fetches random documents from a remote API.
 */
public class ScanService {
    private static ScanService instance;
    private final Random random = new Random();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String API_URL = "https://studentiffapi-production.up.railway.app/getRandomFile";

    public static class ScanResult {
        private final String imagePath;
        private final boolean isBarcode;
        private final byte[] data;

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
     * Fetches a random TIFF file from the remote API.
     */
    public ScanResult scan() {
        // Small chance of generating a barcode instead of a document
        if (random.nextInt(10) == 0) {
            String id = String.valueOf(random.nextInt(100000));
            return new ScanResult("BARCODE-" + id, true, null);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                byte[] zipData = response.body();
                try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
                    ZipEntry entry = zis.getNextEntry();
                    if (entry != null) {
                        byte[] tiffData = zis.readAllBytes();
                        return new ScanResult(API_URL, false, tiffData);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }

        // Fallback to simulation if API fails
        String fallbackId = String.valueOf(random.nextInt(100000));
        return new ScanResult("https://picsum.photos/seed/" + fallbackId + "/800/600", false, new byte[0]);
    }
}
