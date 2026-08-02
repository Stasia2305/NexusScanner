package com.nexusscan.dal;

import com.nexusscan.dal.db.LogDAO;
import com.nexusscan.dal.db.MetadataDAO;
import com.nexusscan.dal.db.ProfileDAO;
import com.nexusscan.dal.db.SessionDAO;
import com.nexusscan.dal.db.UserDAO;
import com.nexusscan.dal.interfaces.ILogDAO;
import com.nexusscan.dal.interfaces.IMetadataDAO;
import com.nexusscan.dal.interfaces.IProfileDAO;
import com.nexusscan.dal.interfaces.ISessionDAO;
import com.nexusscan.dal.interfaces.IUserDAO;

/**
 * Factory class for creating DAO instances.
 * This implements the Factory pattern to decouple the logic layer from the data access implementation.
 */
public class DAOFactory {
    
    public static IUserDAO getUserDAO() {
        return new UserDAO();
    }

    public static IProfileDAO getProfileDAO() {
        return new ProfileDAO();
    }

    public static IMetadataDAO getMetadataDAO() {
        return new MetadataDAO();
    }

    public static ILogDAO getLogDAO() {
        return new LogDAO();
    }

    public static ISessionDAO getSessionDAO() {
        return new SessionDAO();
    }
}
