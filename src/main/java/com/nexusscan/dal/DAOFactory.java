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
    
    /**
     * Retrieves an implementation of IUserDAO.
     *
     * @return A new instance of IUserDAO.
     */
    public static IUserDAO getUserDAO() {
        return new UserDAO();
    }

    /**
     * Retrieves an implementation of IProfileDAO.
     *
     * @return A new instance of IProfileDAO.
     */
    public static IProfileDAO getProfileDAO() {
        return new ProfileDAO();
    }

    /**
     * Retrieves an implementation of IMetadataDAO.
     *
     * @return A new instance of IMetadataDAO.
     */
    public static IMetadataDAO getMetadataDAO() {
        return new MetadataDAO();
    }

    /**
     * Retrieves an implementation of ILogDAO.
     *
     * @return A new instance of ILogDAO.
     */
    public static ILogDAO getLogDAO() {
        return new LogDAO();
    }

    /**
     * Retrieves an implementation of ISessionDAO.
     *
     * @return A new instance of ISessionDAO.
     */
    public static ISessionDAO getSessionDAO() {
        return new SessionDAO();
    }
}
