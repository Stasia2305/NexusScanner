package com.nexusscan.logic;

import com.nexusscan.dal.DAOFactory;
import com.nexusscan.dal.interfaces.ISessionDAO;
import com.nexusscan.model.Document;
import com.nexusscan.model.Profile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The ScanService simulates a physical document scanner.
 * It interacts with a remote API to fetch random TIFF files and barcodes.
 * This ensures the application can be demonstrated across different operating systems.
 */
public class ScanService {
    private static ScanService instance;
    private final Random random = new Random();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    // Endpoint for retrieving simulated scanned documents
    private static final String API_URL = "https://studentiffapi-production.up.railway.app/getRandomFile";
    private final ISessionDAO sessionDAO;

    /**
     * Inner class representing the output of a single page scan.
     */
    public static class ScanResult {
        private final String imagePath;
        private final boolean isBarcode; // True if the result represents a document separator
        private final byte[] data; // Raw TIFF image data

        /**
         * Constructs a new ScanResult.
         *
         * @param imagePath The path or URL of the scanned image.
         * @param isBarcode True if this scan result represents a barcode separator.
         * @param data      The raw byte data of the scanned TIFF file.
         */
        public ScanResult(String imagePath, boolean isBarcode, byte[] data) {
            this.imagePath = imagePath;
            this.isBarcode = isBarcode;
            this.data = data;
        }

        /**
         * Gets the path to the scanned image.
         *
         * @return The image path or URL.
         */
        public String getImagePath() { return imagePath; }

        /**
         * Indicates whether the scan is a barcode separator.
         *
         * @return True if barcode, false if normal page.
         */
        public boolean isBarcode() { return isBarcode; }

        /**
         * Gets the raw byte data of the scan.
         *
         * @return Raw image data byte array.
         */
        public byte[] getData() { return data; }
    }

    /**
     * Private constructor for singleton pattern.
     */
    private ScanService() {
        this.sessionDAO = DAOFactory.getSessionDAO();
    }

    /**
     * Gets the Singleton instance of ScanService.
     *
     * @return The ScanService instance.
     */
    public static synchronized ScanService getInstance() {
        if (instance == null) {
            instance = new ScanService();
        }
        return instance;
    }

    /**
     * Simulates scanning a single sheet of paper by fetching a random TIFF file
     * or generating a barcode from the remote API.
     *
     * @return A ScanResult representing the scanned page, or null if scanning failed.
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
            System.err.println("IOException during scanning simulation: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Scanning simulation thread was interrupted.");
            Thread.currentThread().interrupt();
        }
        return null;
    }

    /**
     * Exports a scanning session to the database.
     *
     * @param profile     The profile used for scanning.
     * @param boxIdStr    The identifier for the physical box.
     * @param metadataStr Serialized metadata for the session.
     * @param documents   The list of documents and pages to save.
     * @throws SQLException If database execution fails.
     */
    public void exportSession(Profile profile, String boxIdStr, String metadataStr, List<Document> documents) throws SQLException {
        sessionDAO.exportSession(profile, boxIdStr, metadataStr, documents);
    }
}
