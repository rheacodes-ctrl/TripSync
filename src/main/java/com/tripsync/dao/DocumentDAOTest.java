
package com.tripsync.dao;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class DocumentDAOTest {

    public static void main(String[] args) throws Exception {

        DocumentDAO dao = new DocumentDAO();

        String documentId = null;
        Path temporaryFile = null;

        System.out.println("========== DOCUMENT DAO TEST ==========");

        try {

            // Create a temporary source document.
            temporaryFile = Files.createTempFile(
                "tripsync-test-", ".txt"
            );

            Files.writeString(
                temporaryFile,
                "This is a test document for TripSync."
            );

            // 1. Upload document.
            System.out.println("\n========== UPLOAD DOCUMENT ==========");

            documentId = dao.uploadDocument(
                "T001",
                "U001",
                temporaryFile.toString()
            );

            System.out.println("Document ID: " + documentId);

            // 2. Retrieve document.
            System.out.println("\n========== RETRIEVE DOCUMENT ==========");

            Map<String, Object> document =
                    dao.getDocumentById(documentId);

            System.out.println("Document: " + document);

            System.out.println(
                "Correct ID: " +
                documentId.equals(document.get("documentId"))
            );

            // 3. List trip documents.
            System.out.println("\n========== TRIP DOCUMENTS ==========");

            List<Map<String, Object>> documents =
                    dao.getDocumentsByTripId("T001");

            for (Map<String, Object> item : documents) {
                System.out.println(item);
            }

            System.out.println(
                "Documents for trip: " + documents.size()
            );

            // 4. List all documents.
            System.out.println("\n========== ALL DOCUMENTS ==========");

            System.out.println(
                "Total documents: " + dao.getDocumentCount()
            );

            // 5. Check document existence.
            System.out.println("\n========== CHECK DOCUMENT ==========");

            System.out.println(
                "Document exists: " +
                dao.documentExists(documentId)
            );

            // 6. Delete test document.
            System.out.println("\n========== DELETE DOCUMENT ==========");

            boolean deleted = dao.deleteDocument(documentId);

            System.out.println("Document deleted: " + deleted);

            System.out.println(
                "Exists after deletion: " +
                dao.documentExists(documentId)
            );

            System.out.println("\nDOCUMENT DAO TEST COMPLETED");

        } finally {

            // Remove temporary source file.
            if (temporaryFile != null) {
                Files.deleteIfExists(temporaryFile);
            }
        }
    }
}
