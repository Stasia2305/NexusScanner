package com.nexusscan.service;

import com.nexusscan.dal.DAOFactory;
import com.nexusscan.dal.interfaces.IProfileDAO;
import com.nexusscan.model.Profile;

import java.sql.SQLException;
import java.util.List;

public class ProfileService {
    private final IProfileDAO profileDAO;

    public ProfileService() {
        this.profileDAO = DAOFactory.getProfileDAO();
    }

    public List<Profile> getAllProfiles() throws SQLException {
        return profileDAO.getAllProfiles();
    }

    public List<Profile> getAccessibleProfiles(String username) throws SQLException {
        return profileDAO.getUserProfiles(username);
    }

    public void createProfile(Profile profile) throws SQLException {
        profileDAO.addProfile(profile);
    }

    public void assignProfileToUser(String username, String profileName) throws SQLException {
        profileDAO.assignProfileToUser(username, profileName);
    }

    public void removeProfileFromUser(String username, String profileName) throws SQLException {
        profileDAO.removeProfileFromUser(username, profileName);
    }

    public void deleteProfile(String name) throws SQLException {
        profileDAO.deleteProfile(name);
    }
}
