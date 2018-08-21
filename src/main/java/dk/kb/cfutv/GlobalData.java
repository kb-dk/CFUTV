package dk.kb.cfutv;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: asj
 * Date: 28-08-12
 * Time: 13:42
 * To change this template use File | Settings | File Templates.
 */
public class GlobalData{
    private static List<String> allowedChannels;
    private static String pathToTemplate;
    private static String youSeeAccessUrl;
    private static String downloadDestination;
    private static String daysBackRaw;
    private static int daysAhead;

    public static String getDownloadDestination() {
        return downloadDestination;
    }

    public static void setDownloadDestination(String downloadDestination) {
        GlobalData.downloadDestination = downloadDestination;
    }

    public static String getYouSeeAccessUrl() {
        return youSeeAccessUrl;
    }

    public static void setYouSeeAccessUrl(String youseeAccessUrl) {
        GlobalData.youSeeAccessUrl = youseeAccessUrl;
    }

    /**
     * Returns the path to the PBCore template.
     * @return String path to PBCore template
     */
    public static String getPathToTemplate() {
        return pathToTemplate;
    }

    /**
     * Sets the path to the PBCore template.
     * @param pathToTemplate path
     */
    public static void setPathToTemplate(String pathToTemplate) {
        GlobalData.pathToTemplate = pathToTemplate;
    }

    /**
     * Returns a List containing the channels to be searched.
     * @return a List of allowed channels
     */
    public static List<String> getAllowedChannels() {
        return allowedChannels;
    }

    /**
     * Sets which channels are to be searched.
     * @param allowedChannels list of channels
     */
    public static void setAllowedChannels(List<String> allowedChannels) {
        GlobalData.allowedChannels = allowedChannels;
    }

    /**
     * Returns a Date telling how many days back a search is allowed to go.
     * @return Date days back calculated every time
     */
    public static Date getDaysBack() {
    	int daysBackInt = Integer.parseInt(daysBackRaw);
    	Calendar cal = Calendar.getInstance();
    	cal.setTime(new Date());
    	cal.add(Calendar.DATE, -daysBackInt);
        return cal.getTime();
    }

	public static String getDaysBackRaw() {
		return daysBackRaw;
	}
	
	public static void setDaysBackRaw(String daysBackRaw) {
		GlobalData.daysBackRaw = daysBackRaw;
	}
	
	public static int getDaysAhead() {
	    return daysAhead;
	}
	
	public static void setDaysAhead(int days) {
	    GlobalData.daysAhead = days;
	}
}
