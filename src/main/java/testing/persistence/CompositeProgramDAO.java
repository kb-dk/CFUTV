package testing.persistence;

import java.util.List;

import dk.statsbiblioteket.digitaltv.access.model.CompositeProgram;
import dk.statsbiblioteket.digitaltv.access.model.RitzauProgram;
import dk.statsbiblioteket.mediaplatform.ingest.model.persistence.GenericHibernateDAO;
import dk.statsbiblioteket.mediaplatform.ingest.model.persistence.HibernateUtilIF;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * Created with IntelliJ IDEA.
 * User: asj
 * Date: 21-09-12
 * Time: 11:03
 * To change this template use File | Settings | File Templates.
 */
public class CompositeProgramDAO extends GenericHibernateDAO<RitzauProgram, Long> {
    public CompositeProgramDAO(HibernateUtilIF util){
        super(RitzauProgram.class, util);
    }

    public boolean hasTvMeter(RitzauProgram ritzauProgram){
        Criteria criteria = getSession().createCriteria(CompositeProgram.class);
        Criterion criterion = Restrictions.eq("ritzauProgram", ritzauProgram);
        criteria.add(criterion);
        CompositeProgram compositeProgram = (CompositeProgram) criteria.uniqueResult();
        if(compositeProgram != null){
            if(compositeProgram.getTvmeterProgram() != null){
                return true;
            }
        }
        return false;
    }
    /**
     * Returns the single CompositeProgram corresponding to the given Ritzau Program if
     * it exists or null otherwise.
     * @param rp ritzauprogram to match
     * @return a composite program if found
     */
    public CompositeProgram getCorrespondingCompositeProgram(RitzauProgram rp) {
        List<CompositeProgram> allCompositePrograms =  getSession().createQuery(
                "from CompositeProgram where ritzauProgram = :rp")
                .setParameter("rp", rp).list();
        if (!allCompositePrograms.isEmpty()) {
            return allCompositePrograms.get(0);
        } else {
            return null;
        }
    }
}
