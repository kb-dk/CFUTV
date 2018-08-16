package testing.persistence;

import java.util.Date;
import java.util.List;

import org.hibernate.SQLQuery;
import org.hibernate.Session;

import dk.statsbiblioteket.digitaltv.access.model.RitzauProgram;
import dk.statsbiblioteket.mediaplatform.ingest.model.persistence.GenericHibernateDAO;
import dk.statsbiblioteket.mediaplatform.ingest.model.persistence.HibernateUtilIF;
import testing.GlobalData;

/**
 * Created with IntelliJ IDEA.
 * User: asj
 * Date: 16-08-12
 * Time: 11:16
 * To change this template use File | Settings | File Templates.
 */
public class CfuTvDAO extends GenericHibernateDAO<RitzauProgram, Long> {
    public CfuTvDAO(HibernateUtilIF util){
        super(RitzauProgram.class, util);
    }

    protected String buildSearchSQL(String channel_name, Date from, Date to, String title, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM cfuritzau ");
        sb.append("WHERE publishable IS TRUE "); 
        sb.append("AND channel_name IN (:channels) ");
        sb.append("AND starttid >= :starttid");
        
        if(channel_name != null && channel_name.trim().length() != 0){
            sb.append(" AND channel_name = :channel_name");
        }
        if(from != null){
            sb.append(" AND starttid >= :from");
        }
        if(to != null){
            sb.append(" AND starttid <= :to");
        }
        if(title != null && title.trim().length() != 0){
            sb.append(" AND titel like :title");
        }
        if(description != null && description.trim().length() != 0){
            sb.append(" AND (langomtale1 like :description");
            sb.append(" OR kortomtale like :description");
            sb.append(" OR langomtale2 like :description)");
        }
        
        return sb.toString();
    }
    
    protected void addParametersToQuery(SQLQuery query, String channel_name, Date from, Date to, String title, 
            String description) {
        query.setParameterList("channels", GlobalData.getAllowedChannels());
        query.setParameter("starttid", GlobalData.getDaysBack());
        if(channel_name != null && channel_name.trim().length() != 0){
            query.setParameter("channel_name", channel_name);
        }
        if(from != null){
            query.setParameter("from", from);
        }
        if(to != null){
            query.setParameter("to", to);
        }
        if(title != null && title.trim().length() != 0){
            query.setParameter("title", "%" + title + "%");
        }
        if(description != null && description.trim().length() != 0){
            query.setParameter("description", "%" + description + "%");
        }
    }
    
    /**
     * Search for RitzauPrograms in the database and return a list of programs matching input data.
     * @param channel_name name for channel (mapped)
     * @param from date
     * @param to date
     * @param title titel
     * @param description Kortomtale, langomtale1 eller langomtale2.
     * @return List of RitzauPrograms matching input data
     */
    @SuppressWarnings("unchecked")
    public List<RitzauProgram> search(String channel_name, Date from, Date to, String title, String description){
        Session session = null;
        try{
            session = getSession();
            SQLQuery query = session.createSQLQuery(buildSearchSQL(channel_name, from, to, title, description));
            addParametersToQuery(query, channel_name, from, to, title, description);
            
            List<RitzauProgram> programs = query.addEntity(RitzauProgram.class).list();
                   
            return programs;
        }
        finally {
            if(session != null)
                session.close();
        }
    }
    
    /**
     * Finds and returns a single RitzauProgram based on id.
     * @param id which program.
     * @return Found RitzauProgram.
     */
    public RitzauProgram getByFullId(Long id){
        Session session = null;
        if(id == null) {
        	throw new IllegalStateException("ID was null, should not happen!");
        }
        try{
            session = getSession();
            String sqlQuery = "SELECT * FROM cfuritzau WHERE starttid >= :starttid AND id = :id";
            SQLQuery query = session.createSQLQuery(sqlQuery);
            
            query.setParameter("starttid", GlobalData.getDaysBack());
            query.setParameter("id", id);
                        
            RitzauProgram program = (RitzauProgram) query.addEntity(RitzauProgram.class).uniqueResult();
            return program;
        }
        finally {
            if(session != null)
                session.close();
        }
    }
}
