package dk.kb.cfutv.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class RitzauHarvestUtil {

    private static final int RITZAU_DAY_START_HOUR = 6;
    
    /**
     * Break interval represented by start and stop into slices cut around ritzau day start.
     * @param start The start of the interval
     * @param stop The end of the interval
     * @return list of HarvestTimeSlice containing the slices of time between start and stop  
     */
    public static List<HarvestTimeSlice> createHarvestSlices(Date start, Date stop) {
        if(start.after(stop)) {
            throw new IllegalStateException("Start cannot be after stop");
        }
        
        Calendar cal = Calendar.getInstance();
        List<HarvestTimeSlice> slices = new ArrayList<>();
        
        Date from;
        Date to;
        
        from = start;
        to = getFirstToDate(start, stop);
        
        slices.add(new HarvestTimeSlice(from, to));
        
        // Fast path if only one slice is needed. 
        if(to.equals(stop)) {
            return slices;
        } else {
            cal.setTime(to);    
        }
        
        while(cal.getTime().before(stop)) {
            from = cal.getTime();
            cal.add(Calendar.DATE, 1);
            to = cal.getTime();
            if(to.after(stop)) {
                to = stop;
            }
            slices.add(new HarvestTimeSlice(from, to));
        }
        
        return slices;
        
    }
    
    protected static Date getFirstToDate(Date start, Date stop) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(start);
        Date to;
        if(cal.get(Calendar.HOUR_OF_DAY) < RITZAU_DAY_START_HOUR) { 
            cal.set(Calendar.HOUR_OF_DAY, RITZAU_DAY_START_HOUR);
            to = cal.getTime();
        } else {
            cal.set(Calendar.HOUR_OF_DAY, RITZAU_DAY_START_HOUR);
            cal.add(Calendar.DATE, 1);
            to = cal.getTime();
        }
        
        return to.before(stop) ? to : stop;
    }
    
}
