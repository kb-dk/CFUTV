package dk.kb.cfutv.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RitzauHarvestUtilTest {

    /*
     * Tests:
     * Small interval: 2000-01-01T04:00 2000-01-01T05:00
     * Small interval: 2000-01-01T04:00 2000-01-01T07:00
     * Slightly bigger interval: 2000-01-01T04:00 2000-01-02T05:00  
     * Slightly bigger interval: 2000-01-01T04:00 2000-01-02T07:00
     * Pretty interval small: 2000-01-01T06:00 2000-01-02T06:00
     * Pretty interval slightly bigger: 2000-01-01T06:00 2000-01-03T06:00
     * Bigger interval: 2000-01-01T06:00 2000-01-03T05:00
     * 
     */
    
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
    
    @Test
    public void verySmallIntervalTest() throws ParseException {
        Date start = format.parse("2000-01-01T04:00");
        Date stop = format.parse("2000-01-01T05:00");
        
        List<HarvestTimeSlice> slices = RitzauHarvestUtil.createHarvestSlices(start, stop);
        
        Assert.assertEquals(slices.size(), 1, "Should only have one timeslice");
        HarvestTimeSlice slice = slices.get(0);
        Assert.assertEquals(slice.getFrom(), start, "From time should be start");
        Assert.assertEquals(slice.getTo(), stop, "To time should be stop"); 
    }
    
    @Test
    public void smallIntervalTest() throws ParseException {
        Date start = format.parse("2000-01-01T04:00");
        Date stop = format.parse("2000-01-01T07:00");
        Date split = format.parse("2000-01-01T06:00");
        
        List<HarvestTimeSlice> slices = RitzauHarvestUtil.createHarvestSlices(start, stop);
        
        Assert.assertEquals(slices.size(), 2, "Should only have two timeslice");
        HarvestTimeSlice slice = slices.get(0);
        Assert.assertEquals(slice.getFrom(), start, "From time should be start");
        Assert.assertEquals(slice.getTo(), split, "To time should be split");
        
        slice = slices.get(1);
        Assert.assertEquals(slice.getFrom(), split, "From time should be split");
        Assert.assertEquals(slice.getTo(), stop, "To time should be stop");
    }
    
    @Test
    public void slightlyBiggerIntervalTest() throws ParseException {
        Date start = format.parse("2000-01-01T04:00");
        Date stop = format.parse("2000-01-02T05:00");
        Date split = format.parse("2000-01-01T06:00");
        
        List<HarvestTimeSlice> slices = RitzauHarvestUtil.createHarvestSlices(start, stop);
        
        Assert.assertEquals(slices.size(), 2, "Should only have two timeslice");
        HarvestTimeSlice slice = slices.get(0);
        Assert.assertEquals(slice.getFrom(), start, "From time should be start");
        Assert.assertEquals(slice.getTo(), split, "To time should be split");
        
        slice = slices.get(1);
        Assert.assertEquals(slice.getFrom(), split, "From time should be split");
        Assert.assertEquals(slice.getTo(), stop, "To time should be stop");
    }
    
    @Test
    public void slightlyBiggerInterval2Test() throws ParseException {
        Date start = format.parse("2000-01-01T04:00");
        Date stop = format.parse("2000-01-02T07:00");
        Date split1 = format.parse("2000-01-01T06:00");
        Date split2 = format.parse("2000-01-02T06:00");
        
        List<HarvestTimeSlice> slices = RitzauHarvestUtil.createHarvestSlices(start, stop);
        
        Assert.assertEquals(slices.size(), 3, "Should only have two timeslice");
        HarvestTimeSlice slice = slices.get(0);
        Assert.assertEquals(slice.getFrom(), start, "From time should be start");
        Assert.assertEquals(slice.getTo(), split1, "To time should be split1");
        
        slice = slices.get(1);
        Assert.assertEquals(slice.getFrom(), split1, "From time should be split1");
        Assert.assertEquals(slice.getTo(), split2, "To time should be split2");
        
        slice = slices.get(2);
        Assert.assertEquals(slice.getFrom(), split2, "From time should be split2");
        Assert.assertEquals(slice.getTo(), stop, "To time should be stop");
    }
    
    @Test
    public void smallPrettyIntervalTest() throws ParseException {
        Date start = format.parse("2000-01-01T06:00"); 
        Date stop = format.parse("2000-01-02T06:00");
        
        List<HarvestTimeSlice> slices = RitzauHarvestUtil.createHarvestSlices(start, stop);
        
        Assert.assertEquals(slices.size(), 1, "Should only have one timeslice");
        HarvestTimeSlice slice = slices.get(0);
        Assert.assertEquals(slice.getFrom(), start, "From time should be start");
        Assert.assertEquals(slice.getTo(), stop, "To time should be stop"); 
    }
    
    @Test
    public void prettyIntervalTest() throws ParseException {
        Date start = format.parse("2000-01-01T06:00"); 
        Date stop = format.parse("2000-01-03T06:00");
        Date split = format.parse("2000-01-02T06:00");
        
        List<HarvestTimeSlice> slices = RitzauHarvestUtil.createHarvestSlices(start, stop);
        
        Assert.assertEquals(slices.size(), 2, "Should only have two timeslice");
        HarvestTimeSlice slice = slices.get(0);
        Assert.assertEquals(slice.getFrom(), start, "From time should be start");
        Assert.assertEquals(slice.getTo(), split, "To time should be split");
        
        slice = slices.get(1);
        Assert.assertEquals(slice.getFrom(), split, "From time should be split");
        Assert.assertEquals(slice.getTo(), stop, "To time should be stop"); 
    }
    
    @Test
    public void slightlyBiggerInterval3Test() throws ParseException {
        Date start = format.parse("2000-01-01T06:00"); 
        Date stop = format.parse("2000-01-03T05:00");
        Date split = format.parse("2000-01-02T06:00");
        
        List<HarvestTimeSlice> slices = RitzauHarvestUtil.createHarvestSlices(start, stop);
        
        Assert.assertEquals(slices.size(), 2, "Should only have two timeslice");
        HarvestTimeSlice slice = slices.get(0);
        Assert.assertEquals(slice.getFrom(), start, "From time should be start");
        Assert.assertEquals(slice.getTo(), split, "To time should be split");
        
        slice = slices.get(1);
        Assert.assertEquals(slice.getFrom(), split, "From time should be split");
        Assert.assertEquals(slice.getTo(), stop, "To time should be stop"); 
    }
    
}
