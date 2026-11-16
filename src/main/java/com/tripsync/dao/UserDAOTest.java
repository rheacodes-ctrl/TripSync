
package com.tripsync.dao;

import com.tripsync.model.Traveler;
import com.tripsync.model.TripAdmin;
import com.tripsync.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class UserDAOTest {

    public static void main(String[] args) {

        UserDAO userDAO = new UserDAO();

        // Unique email to avoid conflicts with existing database records
        String uniqueEmail = "test_" + UUID.randomUUID() + "@tripsync.com";

        try {

            // 1. CREATE TRAVELER
            System.out.println("\n========== CREATE TRAVELER ==========");

            Traveler traveler = new Traveler(
                    "Test Traveler",
                    uniqueEmail,
                    "test123456"
            );

            boolean travelerAdded = userDAO.addUser(traveler);

            System.out.println("Traveler inserted: " + travelerAdded);
            System.out.println("Traveler ID: " + traveler.getUserId());

            // 2. CREATE ADMIN
            System.out.println("\n========== CREATE ADMIN ==========");

            String adminEmail =
                    "admin_" + UUID.randomUUID() + "@tripsync.com";

            TripAdmin admin = new TripAdmin(
                    "Test Admin",
                    adminEmail,
                    "admin123456"
            );

            boolean adminAdded = userDAO.addUser(admin);

            System.out.println("Admin inserted: " + adminAdded);

            // 3. RETRIEVE TRAVELER
            System.out.println("\n========== RETRIEVE TRAVELER ==========");

            User retrievedUser =
                    userDAO.getUserById(traveler.getUserId());

            if (retrievedUser != null) {
                retrievedUser.displayProfile();

                System.out.println(
                        "Correct object type: "
                                + (retrievedUser instanceof Traveler)
                );
            }

            // 4. RETRIEVE ALL USERS
            System.out.println("\n========== ALL USERS ==========");

            List<User> users = userDAO.getAllUsers();

            for (User user : users) {
                System.out.println(
                        user.getName() + " | "
                                + user.getEmail() + " | "
                                + user.getRole()
                );
            }

            System.out.println("Total users: " + users.size());

            // 5. UPDATE TRAVELER
            System.out.println("\n========== UPDATE TRAVELER ==========");

            traveler.setName("Updated Traveler");

            boolean updated = userDAO.updateUser(traveler);

            System.out.println("Traveler updated: " + updated);

            User updatedUser =
                    userDAO.getUserById(traveler.getUserId());

            System.out.println(
                    "Updated name: " + updatedUser.getName()
            );

            // 6. RETRIEVE ADMIN
            System.out.println("\n========== RETRIEVE ADMIN ==========");

            User retrievedAdmin =
                    userDAO.getUserById(admin.getUserId());

            if (retrievedAdmin != null) {
                retrievedAdmin.displayProfile();

                System.out.println(
                        "Correct object type: "
                                + (retrievedAdmin instanceof TripAdmin)
                );
            }

            // 7. DELETE TEST RECORDS
            System.out.println("\n========== DELETE TEST RECORDS ==========");

            boolean travelerDeleted =
                    userDAO.deleteUser(traveler.getUserId());

            boolean adminDeleted =
                    userDAO.deleteUser(admin.getUserId());

            System.out.println("Traveler deleted: " + travelerDeleted);
            System.out.println("Admin deleted: " + adminDeleted);

            System.out.println("\n========== DAO TEST COMPLETED ==========");

        } catch (SQLException e) {

            System.err.println("Database operation failed:");
            e.printStackTrace();

        } catch (Exception e) {

            System.err.println("Test failed:");
            e.printStackTrace();
        }
    }
}
