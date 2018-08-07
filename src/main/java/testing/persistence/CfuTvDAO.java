package testing.persistence;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

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
            sb.append(" AND title like %:title%");
        }
        if(description != null && description.trim().length() != 0){
            sb.append(" AND (langomtale1 like %:description%");
            sb.append("OR kortomtale like %:description%");
            sb.append("OR langomtale2 like %:description%)");
        }
        
        sb.append(" ORDER BY starttid ASC");
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
            query.setParameter("title", title);
        }
        if(description != null && description.trim().length() != 0){
            query.setParameter("description", description);
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
    public List<RitzauProgram> newsearch(String channel_name, Date from, Date to, String title, String description){
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
	        Criteria criteria = session.createCriteria(RitzauProgram.class);
	        List<String> list = GlobalData.getAllowedChannels();
	        Criterion isPublishable = Restrictions.eq("publishable", true);
	        criteria.add(isPublishable);
	        Criterion sbChannelId = Restrictions.in("channel_name",list);
	        criteria.add(sbChannelId);
	        Criterion daysBack = Restrictions.ge("starttid", GlobalData.getDaysBack());
	        criteria.add(daysBack);
	        if(channel_name != null && channel_name.trim().length() != 0){
	            Criterion channel_criterion = Restrictions.eq("channel_name",channel_name);
	            criteria.add(channel_criterion);
	        }
	        if(from != null){
	            Criterion from_criterion = Restrictions.ge("starttid",from);
	            criteria.add(from_criterion);
	        }
	        if(to != null){
	            Criterion to_criterion = Restrictions.le("starttid", to);
	            criteria.add(to_criterion);
	        }
	        if(title != null && title.trim().length() != 0){
	            Criterion title_criterion = Restrictions.ilike("titel",
	                    MatchMode.ANYWHERE.toMatchString(title), MatchMode.ANYWHERE);
	            criteria.add(title_criterion);
	        }
	        if(description != null && description.trim().length() != 0){
	            Criterion description_criterion1 = Restrictions.ilike("langomtale1",
	                    MatchMode.ANYWHERE.toMatchString(description), MatchMode.ANYWHERE);
	            Criterion description_criterion2 = Restrictions.ilike("kortomtale",
	                    MatchMode.ANYWHERE.toMatchString(description), MatchMode.ANYWHERE);
	            Criterion description_criterion3 = Restrictions.ilike("langomtale2",
	                    MatchMode.ANYWHERE.toMatchString(description), MatchMode.ANYWHERE);
	            Criterion joint_description_criterion = Restrictions.or(description_criterion1,
	                    Restrictions.or(description_criterion2, description_criterion3));
	            criteria.add(joint_description_criterion);
	        }
	        return (List<RitzauProgram>) criteria.addOrder(Order.asc("starttid")).list();
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
    	try{
			session = getSession();
			Criteria criteria = session.createCriteria(RitzauProgram.class);
			Criterion daysBack = Restrictions.ge("starttid", GlobalData.getDaysBack());
			criteria.add(daysBack);
			if (id != null) {
				Criterion id_criterion = Restrictions.eq("id", id);
				criteria.add(id_criterion);
			}
			return (RitzauProgram) criteria.uniqueResult();
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
    public RitzauProgram newgetByFullId(Long id){
        Session session = null;
        try{
            session = getSession();
            String baseQuery = "SELECT * FROM cfuritzau WHERE starttid >= :starttid";
            String sqlQuery;
            if(id != null) {
                sqlQuery = baseQuery + " AND id = :id";
            } else {
                sqlQuery = baseQuery;
            }
            SQLQuery query = session.createSQLQuery(sqlQuery);
            
            query.setParameter("starttid", GlobalData.getDaysBack());
            if(id != null) {
                query.setParameter("id", id);
            }            
            RitzauProgram program = (RitzauProgram) query.addEntity(RitzauProgram.class).uniqueResult();
            return program;
        }
        finally {
            if(session != null)
                session.close();
        }
    }
}
