package dk.kb.cfutv.web;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 *  Wrapper class to handle initialization by Apache CXF.
 */
@ApplicationPath("/")
public class CFUTVApplication extends Application {
    
    public Set<Class<?>> getClasses() {
        return new HashSet<>(Arrays.asList(CFUTV.class, JacksonJsonProvider.class));
    }

}
