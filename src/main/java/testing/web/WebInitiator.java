package testing.web;

import ch.qos.logback.core.joran.spi.JoranException;
import testing.GlobalData;
import testing.persistence.CfuTvHibernateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.util.Arrays;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: asj
 * Date: 20-08-12
 * Time: 09:43
 * To change this template use File | Settings | File Templates.
 */
public class WebInitiator implements ServletContextListener {
    private Logger log;

    @Override
    public void contextInitialized(ServletContextEvent sce){
        //Logger
        try{
            new LogbackConfigLoader("C:\\Users\\asj\\Desktop\\JerseyTest\\logback.xml");
        } catch(JoranException ex){
            System.out.println(ex.getMessage() + " " + ex.getStackTrace());
        }
        log = LoggerFactory.getLogger(WebInitiator.class);

        //Hibernate
        String cfgPath = sce.getServletContext().getInitParameter("hibernate_cfg");
        System.out.println("------------------------------------");
        System.out.println("config path: '"+cfgPath + "'");
        System.out.println("------------------------------------");
        final File cfgFile = new File(cfgPath);
        log.info("Reading hibernate configuration from " + cfgFile.getAbsolutePath());
        CfuTvHibernateUtil util = CfuTvHibernateUtil.initialiseFactory(cfgFile);
        util.getSession();

        //Allowed channels
        String allowedChannels = sce.getServletContext().getInitParameter("allowed_channels");
        //Restrictions.in requires input collection to have the .toArray method
        GlobalData.setAllowedChannels(Arrays.asList(allowedChannels.split(",")));

        //Number of days back in time
        String daysBackRaw = sce.getServletContext().getInitParameter("days_back");
        int daysBackInt = Integer.parseInt(daysBackRaw);
        Date daysBack = new Date();
        daysBack.setDate(daysBack.getDate()-daysBackInt);
        GlobalData.setDaysBack(daysBack);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce){

    }
}
