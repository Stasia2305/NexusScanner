package com.nexusscan.dal.interfaces;

import com.nexusscan.model.Profile;
import java.sql.SQLException;
import java.util.List;

public interface IProfileDAO {
    List<Profile> getAllProfiles() throws SQLException;
    void addProfile(Profile profile) throws SQLException;
    void deleteProfile(String name) throws SQLException;
    List<Profile> getUserProfiles(String username) throws SQLException;
    void assignProfileToUser(String username, String profileName) throws SQLException;
}
