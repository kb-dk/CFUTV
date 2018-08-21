package dk.kb.cfutv.web;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Loads a Logback configurations.
 */
public class LogbackConfigLoader {
    /** The log.*/
    private Logger log = LoggerFactory.getLogger(LogbackConfigLoader.class);

    /**
     * Constructor.
     * @param configFileLocation The path to the configuration file.
     * @throws JoranException If the configuration cannot be loaded.
     */
    public LogbackConfigLoader(String configFileLocation) throws JoranException{
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        File configFile = new File(configFileLocation);
        if(!configFile.isFile()){
            throw new IllegalArgumentException("Logback External Config File Parameter, "
                    + configFile.getAbsolutePath() + ", is not a file (either does not exists or is a directory).");
        }

        if(!configFile.canRead()){
            throw new IllegalArgumentException("Logback External Config File cannot be read from '"
                    + configFile.getAbsolutePath() + "'");
        }

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        loggerContext.reset();
        configurator.doConfigure(configFileLocation);
        log.info("Configured Logback with config file from: " + configFileLocation);
    }
}

