package dk.kb.cfutv.utils;

import java.time.ZonedDateTime;


/**
 * Class to represent a harvest slice (used for getting importing Ritzau from their webservice into Digitaltv database).
 */
public class HarvestTimeSlice {
    private final ZonedDateTime to;
    private final ZonedDateTime from;

    public HarvestTimeSlice(ZonedDateTime from, ZonedDateTime to) {
        this.to = to;
        this.from = from;
    }

    public ZonedDateTime getTo() {
        return to;
    }
    
    public ZonedDateTime getFrom() {
        return from;
    }
}
