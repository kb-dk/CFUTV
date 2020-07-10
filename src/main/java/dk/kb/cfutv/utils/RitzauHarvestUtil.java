package dk.kb.cfutv.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import dk.kb.cfutv.GlobalData;

public class RitzauHarvestUtil {

    private static final int RITZAU_DAY_START_HOUR = 6;
    
    /**
     * Break interval represented by start and stop into slices cut around ritzau day start.
     * @param start The start of the interval
     * @param stop The end of the interval
     * @return list of HarvestTimeSlice containing the slices of time between start and stop  
     */
    public static List<HarvestTimeSlice> createHarvestSlices(ZonedDateTime start, ZonedDateTime stop) {
        if(start.isAfter(stop)) {
            throw new IllegalStateException("Start cannot be after stop");
        }

        ZonedDateTime time;

        List<HarvestTimeSlice> slices = new ArrayList<>();
        
        ZonedDateTime from;
        ZonedDateTime to;
        from = start;
        to = getFirstToDate(start, stop);
        slices.add(new HarvestTimeSlice(from, to));
        
        // Fast path if only one slice is needed.
        if(to.equals(stop)) {
            return slices;
        } else {
            time = to;
        }
        while(time.isBefore(stop)) {
            from = time;
            time = time.plusDays(1);
            to = time;
            if(to.isAfter(stop)) {
                to = stop;
            }
            slices.add(new HarvestTimeSlice(from, to));
        }

        return slices;
        
    }
    
    protected static ZonedDateTime getFirstToDate(ZonedDateTime start, ZonedDateTime stop) {
        ZonedDateTime to;
        ZonedDateTime time = start;
        if(time.getHour() < RITZAU_DAY_START_HOUR) {
            time = time.withHour(RITZAU_DAY_START_HOUR);
            to = time;
        } else {
            time = time.withHour(RITZAU_DAY_START_HOUR);
            time = time.plusDays(1);
            to = time;
        }
        
        return to.isBefore(stop) ? to : stop;
    }
    
    public static ZonedDateTime getLatestAvailableDate() {
        ZonedDateTime time = ZonedDateTime.now();
        time.plusDays(GlobalData.getDaysAhead());
        time.plusHours(RITZAU_DAY_START_HOUR);
        return time;
    }
}
