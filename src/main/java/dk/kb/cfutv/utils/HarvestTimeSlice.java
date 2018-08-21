package dk.kb.cfutv.utils;

import java.util.Date;


/**
 * Class to represent a harvest slice (used for getting importing Ritzau from their webservice into Digitaltv database).
 */
public class HarvestTimeSlice {
    private final Date to;
    private final Date from;

    public HarvestTimeSlice(Date from, Date to) {
        this.to = to;
        this.from = from;
    }

    public Date getTo() {
        return to;
    }
    
    public Date getFrom() {
        return from;
    }
}
