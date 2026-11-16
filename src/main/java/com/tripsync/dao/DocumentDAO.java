
package com.tripsync.dao;

import com.tripsync.config.DBConnection;
import com.tripsync.util.FileManager;

import java.io.IOException;
import java.sql.*;
import java.util.*;

public class DocumentDAO {

    private final FileManager fileManager;

    public DocumentDAO() {
        this.fileManager = new FileManager();
    }

    // ==========================================
    // 1. UPLOAD DOCUMENT AND SAVE METADATA
    // ==========================================

    public String uploadDocument(
            String tripId,
            String uploadedBy,
            String sourcePath
    ) throws SQLException, IOException {

        if (tripId == null || tripId.isBlank()
                || uploadedBy == null || uploadedBy.isBlank()) {
            throw new IllegalArgumentException(
                "Trip ID and uploader ID are required."
            );
        }

        // Copy the physical file into uploads/
        String fileName = fileManager.uploadFile(sourcePath);

        String documentId = UUID.randomUUID().toString();
        String filePath = "uploads/" + fileName;

        String sql = """
            INSERT INTO documents
            (document_id, trip_id, uploaded_by, file_name, file_path)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, documentId);
            statement.setString(2, tripId);
            statement.setString(3, uploadedBy);
            statement.setString(4, fileName);
            statement.setString(5, filePath);

            statement.executeUpdate();

            System.out.println("Document metadata saved successfully.");

            return documentId;
        }
    }

    // ==========================================
    // 2. GET DOCUMENT BY ID
    // ==========================================

    public Map<String, Object> getDocumentById(
            String documentId
    ) throws SQLException {

        String sql = """
            SELECT * FROM documents
            WHERE document_id = ?
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, documentId);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    return mapDocument(rs);
                }
            }
        }

        return null;
    }

    // ==========================================
    // 3. GET ALL DOCUMENTS OF A TRIP
    // ==========================================

    public List<Map<String, Object>> getDocumentsByTripId(
            String tripId
    ) throws SQLException {

        List<Map<String, Object>> documents = new ArrayList<>();

        String sql = """
            SELECT * FROM documents
            WHERE trip_id = ?
            ORDER BY uploaded_at DESC
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, tripId);

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {
                    documents.add(mapDocument(rs));
                }
            }
        }

        return documents;
    }

    // ==========================================
    // 4. GET ALL DOCUMENTS
    // ==========================================

    public List<Map<String, Object>> getAllDocuments()
            throws SQLException {

        List<Map<String, Object>> documents = new ArrayList<>();

        String sql = """
            SELECT * FROM documents
            ORDER BY uploaded_at DESC
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                documents.add(mapDocument(rs));
            }
        }

        return documents;
    }

    // ==========================================
    // 5. DELETE DOCUMENT
    // ==========================================

    public boolean deleteDocument(String documentId)
            throws SQLException, IOException {

        Map<String, Object> document =
                getDocumentById(documentId);

        if (document == null) {
            return false;
        }

        String fileName =
                (String) document.get("fileName");

        String sql = """
            DELETE FROM documents
            WHERE document_id = ?
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, documentId);

            int rowsDeleted = statement.executeUpdate();

            if (rowsDeleted > 0) {

                // Delete physical file if it exists.
                if (fileManager.fileExists(fileName)) {
                    fileManager.deleteFile(fileName);
                }

                return true;
            }
        }

        return false;
    }

    // ==========================================
    // 6. CHECK DOCUMENT EXISTS
    // ==========================================

    public boolean documentExists(String documentId)
            throws SQLException {

        String sql = """
            SELECT COUNT(*) FROM documents
            WHERE document_id = ?
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, documentId);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ==========================================
    // 7. GET DOCUMENT COUNT
    // ==========================================

    public int getDocumentCount() throws SQLException {

        String sql = "SELECT COUNT(*) FROM documents";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ==========================================
    // 8. MAP DATABASE ROW TO JAVA MAP
    // ==========================================

    private Map<String, Object> mapDocument(ResultSet rs)
            throws SQLException {

        Map<String, Object> document = new LinkedHashMap<>();

        document.put("documentId", rs.getString("document_id"));
        document.put("tripId", rs.getString("trip_id"));
        document.put("uploadedBy", rs.getString("uploaded_by"));
        document.put("fileName", rs.getString("file_name"));
        document.put("filePath", rs.getString("file_path"));
        document.put("uploadedAt", rs.getTimestamp("uploaded_at"));

        return document;
    }
}
