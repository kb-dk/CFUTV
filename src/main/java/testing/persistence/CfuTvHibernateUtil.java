package testing.persistence;

import dk.statsbiblioteket.digitaltv.access.model.CompositeProgram;
import dk.statsbiblioteket.digitaltv.access.model.RitzauProgram;
import dk.statsbiblioteket.digitaltv.access.model.TvmeterProgram;
//import dk.statsbiblioteket.mediaplatform.ingest.model.YouSeeChannelMapping;
import dk.statsbiblioteket.mediaplatform.ingest.model.persistence.HibernateUtilIF;
import dk.statsbiblioteket.mediaplatform.ingest.model.persistence.NotInitialiasedException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: asj
 * Date: 16-08-12
 * Time: 10:17
 * To change this template use File | Settings | File Templates.
 */
public class CfuTvHibernateUtil implements HibernateUtilIF {
    private static SessionFactory sessionFactory;

    private CfuTvHibernateUtil() {}

    public static CfuTvHibernateUtil getInitialisedFactory(){
        if(sessionFactory != null && !sessionFactory.isClosed()){
            return new CfuTvHibernateUtil();
        } else {
            throw new NotInitialiasedException("Attempt to access uninitialised Hibernate utility.");
        }
    }

    /**
     * Initialise hibernate from a configuration file on first call. Subsequent calls will not reinitialise the
     * hibernate connection unless the sessionFactory is first closed.
     * @param cfgFile  The configuration file.
     * @return An instance of this class.
     */
    public static CfuTvHibernateUtil initialiseFactory(File cfgFile) {
        if(sessionFactory == null || sessionFactory.isClosed()){
            try{
                if(sessionFactory == null){
                    Configuration configuration = (new Configuration()).configure(cfgFile);
                    configuration.addAnnotatedClass(RitzauProgram.class);
//                    configuration.addAnnotatedClass(YouSeeChannelMapping.class);
                    configuration.addAnnotatedClass(TvmeterProgram.class);
                    configuration.addAnnotatedClass(CompositeProgram.class);
                    sessionFactory = configuration.buildSessionFactory();
                }
            } catch (Throwable ex){
                System.err.println("Initial SessionFactory creation failed. " + ex);
                ex.printStackTrace();
                throw new ExceptionInInitializerError(ex);
            }
        }
        return new CfuTvHibernateUtil();
    }

    /**
     * Gets a session factory
     * @return a SessionFactory
     */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Gets a hibernate session. This class must be initialised before this method is called.
     * @return a Session
     */
    public Session getSession() {
        if(sessionFactory == null || sessionFactory.isClosed()){
            throw new RuntimeException("Attempt to use CfuTvHibernateUtil before it was initialised" +
                    "or after sessionFactory was closed");
        }
        return sessionFactory.openSession();
    }
}
