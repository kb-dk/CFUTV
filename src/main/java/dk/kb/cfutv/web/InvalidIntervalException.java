package dk.kb.cfutv.web;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Created with IntelliJ IDEA.
 * User: asj
 * Date: 20-08-12
 * Time: 15:42
 * To change this template use File | Settings | File Templates.
 */
public class InvalidIntervalException extends WebApplicationException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 2104646790914843847L;
	private String message;

    public InvalidIntervalException() {
        super(Response.Status.BAD_REQUEST);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public InvalidIntervalException(String message) {
        super(Response.Status.BAD_REQUEST);    //To change body of overridden methods use File | Settings | File Templates.
        this.message = message;
    }

    public String getMessage(){
        return message;
    }
}
