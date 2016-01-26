package testing.persistence;

import dk.statsbiblioteket.digitaltv.access.model.RitzauProgram;
import dk.statsbiblioteket.mediaplatform.ingest.model.persistence.GenericHibernateDAO;
import dk.statsbiblioteket.mediaplatform.ingest.model.persistence.HibernateUtilIF;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import testing.GlobalData;

import java.util.Date;
import java.util.List;

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
}
