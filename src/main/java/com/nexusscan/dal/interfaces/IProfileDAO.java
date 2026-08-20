package com.nexusscan.dal.interfaces;

import com.nexusscan.model.Profile;
import java.sql.SQLException;
import java.util.List;

/**
 * Interface for Data Access Objects managing scanning profiles.
 */
public interface IProfileDAO {
    /**
     * Retrieves all scanning profiles defined in the database.
     *
     * @return A list of all available Profile objects.
     * @throws SQLException If a database error occurs.
     */
    List<Profile> getAllProfiles() throws SQLException;

    /**
     * Adds a new scanning profile to the database.
     *
     * @param profile The Profile object to add.
     * @throws SQLException If a database error occurs.
     */
    void addProfile(Profile profile) throws SQLException;

    /**
     * Deletes a scanning profile from the database by name.
     *
     * @param name The name of the profile to delete.
     * @throws SQLException If a database error occurs.
     */
    void deleteProfile(String name) throws SQLException;

    /**
     * Retrieves all profiles assigned to a specific user.
     *
     * @param username The username of the user.
     * @return A list of Profile objects assigned to that user.
     * @throws SQLException If a database error occurs.
     */
    List<Profile> getUserProfiles(String username) throws SQLException;

    /**
     * Assigns a scanning profile to a user.
     *
     * @param username    The username of the user.
     * @param profileName The name of the profile to assign.
     * @throws SQLException If a database error occurs.
     */
    void assignProfileToUser(String username, String profileName) throws SQLException;

    /**
     * Removes an assigned scanning profile from a user.
     *
     * @param username    The username of the user.
     * @param profileName The name of the profile to remove.
     * @throws SQLException If a database error occurs.
     */
    void removeProfileFromUser(String username, String profileName) throws SQLException;
}
