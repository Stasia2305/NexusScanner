package com.nexusscan.logic;

import com.nexusscan.dal.DAOFactory;
import com.nexusscan.dal.interfaces.IProfileDAO;
import com.nexusscan.model.Profile;

import java.sql.SQLException;
import java.util.List;

/**
 * Service class for managing scanning profile business logic.
 */
public class ProfileService {
    private final IProfileDAO profileDAO;

    /**
     * Constructs a new ProfileService and obtains the IProfileDAO implementation.
     */
    public ProfileService() {
        this.profileDAO = DAOFactory.getProfileDAO();
    }

    /**
     * Retrieves all system profiles.
     *
     * @return A list of all Profile objects.
     * @throws SQLException If a database error occurs.
     */
    public List<Profile> getAllProfiles() throws SQLException {
        return profileDAO.getAllProfiles();
    }

    /**
     * Retrieves all profiles assigned to a user.
     * Supports offline fallbacks via the ProfileDAO.
     *
     * @param username The username of the user.
     * @return A list of accessible Profile objects.
     * @throws SQLException If a database error occurs.
     */
    public List<Profile> getAccessibleProfiles(String username) throws SQLException {
        return profileDAO.getUserProfiles(username);
    }

    /**
     * Creates and stores a new scanning profile.
     *
     * @param profile The Profile object to create.
     * @throws SQLException If a database error occurs.
     */
    public void createProfile(Profile profile) throws SQLException {
        profileDAO.addProfile(profile);
    }

    /**
     * Assigns a scanning profile to a user.
     *
     * @param username    The username of the user.
     * @param profileName The name of the profile.
     * @throws SQLException If a database error occurs.
     */
    public void assignProfileToUser(String username, String profileName) throws SQLException {
        profileDAO.assignProfileToUser(username, profileName);
    }

    /**
     * Removes an assigned profile from a user.
     *
     * @param username    The username of the user.
     * @param profileName The name of the profile.
     * @throws SQLException If a database error occurs.
     */
    public void removeProfileFromUser(String username, String profileName) throws SQLException {
        profileDAO.removeProfileFromUser(username, profileName);
    }

    /**
     * Deletes a scanning profile by name.
     *
     * @param name The name of the profile to delete.
     * @throws SQLException If a database error occurs.
     */
    public void deleteProfile(String name) throws SQLException {
        profileDAO.deleteProfile(name);
    }
}
