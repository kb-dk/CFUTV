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

/**
 * Created with IntelliJ IDEA.
 * User: asj
 * Date: 20-08-12
 * Time: 09:43
 * To change this template use File | Settings | File Templates.
 */
public class WebInitiator implements ServletContextListener {
    private Logger log;

    /**
     * Sets up the logger and hibernate.
     * Sets path to PBCore template, which channels are to be searched and how many days back.
     * Also where downloads go and the base part of the YouSee url.
     * All setup information is retrieved from the config file.
     * @param sce
     */
    @Override
    public void contextInitialized(ServletContextEvent sce){
        //Logger
        String logBackPath = sce.getServletContext().getInitParameter("logback_cfg");
        System.out.println("------------------------------------");
        System.out.println("Logback config path: '"+logBackPath + "'");
        System.out.println("------------------------------------");
        try{
            new LogbackConfigLoader(logBackPath);
        } catch(JoranException ex){
            System.out.println(ex.getMessage() + " " + ex.getStackTrace());
        }
        log = LoggerFactory.getLogger(WebInitiator.class);

        //Hibernate
        String cfgPath = sce.getServletContext().getInitParameter("hibernate_cfg");
        System.out.println("------------------------------------");
        System.out.println("Hibernate config path: '"+cfgPath + "'");
        System.out.println("------------------------------------");
        final File cfgFile = new File(cfgPath);
        log.info("Reading hibernate configuration from " + cfgFile.getAbsolutePath());
        CfuTvHibernateUtil util = CfuTvHibernateUtil.initialiseFactory(cfgFile);
        util.getSession();

        //PBCore template
        String templatePath = sce.getServletContext().getInitParameter("template_location");
        GlobalData.setPathToTemplate(templatePath);

        //YouSee url
        String youSeeUrl = sce.getServletContext().getInitParameter("youSeeUrl");
        GlobalData.setYouSeeAccessUrl(youSeeUrl);

        //Download destination
        String downloadDestination = sce.getServletContext().getInitParameter("download_destination");
        GlobalData.setDownloadDestination(downloadDestination);

        //Allowed channels
        String allowedChannels = sce.getServletContext().getInitParameter("allowed_channels");
        //Restrictions.in requires input collection to have the .toArray method
        GlobalData.setAllowedChannels(Arrays.asList(allowedChannels.split(",")));

        //Number of days back in time
        String daysBackRaw = sce.getServletContext().getInitParameter("days_back");
        GlobalData.setDaysBackRaw(daysBackRaw);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce){

    }
}
