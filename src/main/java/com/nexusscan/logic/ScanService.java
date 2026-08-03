package com.nexusscan.logic;

import com.nexusscan.dal.DAOFactory;
import com.nexusscan.dal.interfaces.ISessionDAO;
import com.nexusscan.model.Document;
import com.nexusscan.model.Page;
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

    public static class ScanResult {
        private final String imagePath;
        private final boolean isBarcode; // True if the result represents a document separator
        private final byte[] data; // Raw TIFF image data

        public ScanResult(String imagePath, boolean isBarcode, byte[] data) {
            this.imagePath = imagePath;
            this.isBarcode = isBarcode;
            this.data = data;
        }

        public String getImagePath() { return imagePath; }
        public boolean isBarcode() { return isBarcode; }
        public byte[] getData() { return data; }
    }

    private ScanService() {
        this.sessionDAO = DAOFactory.getSessionDAO();
    }

    public static synchronized ScanService getInstance() {
        if (instance == null) {
            instance = new ScanService();
        }
        return instance;
    }

    /**
     * Simulates scanning a single sheet of paper by fetching a random TIFF file
     * or generating a barcode from the remote API.
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
            // Error handling
        } catch (InterruptedException e) {
            // Error handling
            Thread.currentThread().interrupt();
        }
        return null;
    }

    public void exportSession(Profile profile, String boxIdStr, String metadataStr, List<Document> documents) throws SQLException {
        sessionDAO.exportSession(profile, boxIdStr, metadataStr, documents);
    }
}
