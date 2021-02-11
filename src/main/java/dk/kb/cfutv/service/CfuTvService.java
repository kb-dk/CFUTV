package dk.kb.cfutv.service;

import dk.kb.cfutv.GlobalData;
import dk.kb.cfutv.model.ReducedRitzauProgram;
import dk.kb.cfutv.persistence.CfuTvDAO;
import dk.kb.cfutv.persistence.CfuTvHibernateUtil;
import dk.kb.cfutv.persistence.CompositeProgramDAO;
import dk.statsbiblioteket.digitaltv.access.model.RitzauProgram;
import dk.statsbiblioteket.digitaltv.access.model.TvmeterProgram;
import dk.statsbiblioteket.mediaplatform.ingest.model.YouSeeChannelMapping;
import dk.statsbiblioteket.mediaplatform.ingest.model.persistence.YouSeeChannelMappingDAO;
import dk.statsbiblioteket.mediaplatform.ingest.model.service.ServiceException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class CfuTvService {
    private Logger log;
    private CfuTvDAO cfuTvDAO;
    private YouSeeChannelMappingDAO youSeeChannelMappingDAO;
    private String baseUrl = GlobalData.getYouSeeAccessUrl();
    private String extension = ".ts"; //extension of downloaded file, .ts chosen as it is also used in preexisting yousee stuff

    public CfuTvService(){
        log = LoggerFactory.getLogger(CfuTvService.class);
        cfuTvDAO = getCfuTvDao();
        youSeeChannelMappingDAO = getYouSeeChannelMappingDao();
    }

    /**
     * Search for RitzauPrograms in the database and return a list of a reduced version of RitzauProgram.
     * @param channel_name Which channel to search for.
     * @param from Start of search interval, includes year, month, day, hour and minutes.
     * @param to End of search interval.
     * @param title Which title to search for.
     * @param description Phrase or word in description of the program. Not case sensitive.
     * @return A reduced version of a RitzauProgram containing channel name, id, start and end time, title and short description.
     */
    public List<ReducedRitzauProgram> search(String channel_name, ZonedDateTime from, ZonedDateTime to, String title, String description) {
        ArrayList<ReducedRitzauProgram> results = new ArrayList<>();
        List<RitzauProgram> fullPrograms = cfuTvDAO.search(channel_name, from, to, title, description);
        
        for(RitzauProgram program : fullPrograms) {
            results.add(new ReducedRitzauProgram(program.getChannel_name(), program.getId(), program.getStarttid(),
                        program.getSluttid(), program.getTitel(), program.getKortomtale(), program.getProgram_id()));
        }
        
        return results;
    }

    /**
     * Find a RitzauProgram in the database and uses the PBCoreGenerator to return PBCore xml of found program.
     * @param Id Id of the wanted RitzauProgram.
     * @return full PBCore xml (as defined by the template) of a RitzauProgram.
     * @throws ServiceException if programId is null
     */
    public String getFullPost(Long Id) throws ServiceException{
        if(Id == null){
            throw new ServiceException("Id is null.");
        }
        RitzauProgram program = cfuTvDAO.getByFullId(Id);
        boolean tvMeterAvailable = tvmeterAvailable(program);
        PBCoreGenerator generator = new PBCoreGenerator();
        return generator.generateXmlFromTemplate(program,tvMeterAvailable);
    }
    
    protected boolean tvmeterAvailable(RitzauProgram program) {
        // TODO If tvmeter is needed this needs change
        return false;
    }
    
    protected TvmeterProgram getTvmeterProgram(RitzauProgram program) {
        // TODO If tvmeter is needed this needs change
        return null;
    }

    /**
     * Finds and uploads a program, based on id and offsets, to the FtpServer as a file with requested filename, along
     * with PBCore xml about the program.
     * Returns a status code depending on whether it was successful.
     * @param Id Id of wanted program.
     * @param fileName Wanted filename.
     * @param offsetStart Offset from start of the program.
     * @param offsetEnd Offset from end of the program.
     * @return Status code depending on whether it was successful or not.
     * @throws ServiceException service exception
     */
    public int getProgramSnippet(Long Id, String fileName, ZonedDateTime offsetStart, ZonedDateTime offsetEnd) throws ServiceException{
        log.info("----------------getProgramSnippet method called---------------");
        int statusCode;
        RitzauProgram program;

            program = cfuTvDAO.getByFullId(Id);

        if(program == null){
            return -0; //program is null...which error code would that be?
        }
        String sBChannelId = program.getChannel_name();
        String youSeeChannelId = getYouSeeChannelId(sBChannelId, ZonedDateTime.ofInstant(program.getStarttid().toInstant(), ZoneId.of("Europe/Copenhagen")));
        youSeeChannelId = youSeeChannelId.replaceAll(" ","%20"); //Replace space with http equivalent
        String channelUrlPart = youSeeChannelId + "_"; //ChannelId part of the url.
        ZonedDateTime startTid;
        ZonedDateTime slutTid;
        if(tvmeterAvailable(program)){
            TvmeterProgram tvmeter = getTvmeterProgram(program);
            startTid = convertToUTC(ZonedDateTime.ofInstant(program.getStarttid().toInstant(), ZoneId.of("Europe/Copenhagen")));
            slutTid = convertToUTC(ZonedDateTime.ofInstant(program.getSluttid().toInstant(), ZoneId.of("Europe/Copenhagen")));
        }else{
            startTid = convertToUTC(ZonedDateTime.ofInstant(program.getStarttid().toInstant(), ZoneId.of("Europe/Copenhagen")));
            slutTid = convertToUTC(ZonedDateTime.ofInstant(program.getSluttid().toInstant(), ZoneId.of("Europe/Copenhagen")));
        }
        //offsetting start
        startTid.withSecond(startTid.getSecond() - offsetStart.getSecond());
        startTid.withMinute(startTid.getMinute() - offsetStart.getMinute());
        startTid.withHour(startTid.getHour() - offsetStart.getHour());
        //offsetting end
        slutTid.withSecond(slutTid.getSecond() + offsetEnd.getSecond());
        slutTid.withMinute(slutTid.getMinute() + offsetEnd.getMinute());
        slutTid.withHour(slutTid.getHour() + offsetEnd.getHour());
        //offsetting complete
        String fromUrlPart = dateToUrlPart(startTid) + "_"; //From part of the url.
        String toUrlPart = dateToUrlPart(slutTid) + extension; //To part of the url.
        String downloadUrl = baseUrl + channelUrlPart + fromUrlPart + toUrlPart; //Putting the parts together.
        try {
            String xml = getFullPost(Id);
            statusCode = downloadFileByStream(fileName, downloadUrl, xml); //Actual file handling.
        } catch(ServiceException ex){
            throw new ServiceException(ex);
        }
        return statusCode;
    }

    /**
     * Cuts and uploads a interval on a channel, based on channel and interval, to the FtpServer as a file with
     * requested filename.
     * Returns a status code depending on whether it was successful.
     * @param sBChannelId Which channel.
     * @param fileName Wanted filename.
     * @param from Start of the cut.
     * @param to End of the cut.
     * @return Status code depending on whether it was successful or not.
     * @throws ServiceException service exception
     */
    public int getRawCut(String sBChannelId,String fileName,ZonedDateTime from,ZonedDateTime to) throws ServiceException{
        log.info("----------------getProgramSnippet method called---------------");
        int statusCode;
        String youSeeChannelId;
        try{
            youSeeChannelId = getYouSeeChannelId(sBChannelId,from);
        } catch(ServiceException ex){
            return 400; //sBChannelId not found
        }
        youSeeChannelId = youSeeChannelId.replaceAll(" ","%20"); //Replace space with http equivalent
        String channelUrlPart = youSeeChannelId + "_"; //ChannelId part of the url.
        String fromUrlPart = dateToUrlPart(convertToUTC(from)) + "_"; //From part of the url.
        String toUrlPart = dateToUrlPart(convertToUTC(to)) + extension; //To part of the url.
        String downloadUrl = baseUrl + channelUrlPart + fromUrlPart + toUrlPart; //Putting the parts together.

        statusCode = downloadFileByStream(fileName, downloadUrl,null); //Actual file handling.

        return statusCode;
    }

    /**
     * Returns an ArrayList of all files found at download destination.
     * @return ArrayList of all files found at download destination.
     */
    public ArrayList<File> getStatusAll(){
        String location = GlobalData.getDownloadDestination();
        File dir = new File(location);
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        };
        File[] files = dir.listFiles(filter);
        if(files == null){ //Either dir does not exist or it is not a directory.
            return null;
        }
        return new ArrayList<File>(Arrays.asList(files));
    }

    /**
     * Returns the file found at download destination or null if no file with that filename is found.
     * @param filename wanted file, remember to include extension.
     * @return File found at download destination or null.
     */
    public File getStatus(String filename){
        String location = GlobalData.getDownloadDestination() + filename;
        File result = new File(location);
        return result;
    }

    /**
     * Downloads and places a file and associated xml from inputUrl in download destination with filename.
     * Returns a status code depending on how it went.
     * Status codes:
     * 200: OK.
     * 400: Bad information in url.
     * 404: Content not available in main archive. Try again later.
     * 409: A file with chosen filename already exists on FtpServer.
     * 410: Content not available.
     * 500: Internal server error or unexpected status code.
     * @param filename Wanted file name.
     * @param inputUrl Url to download from.
     * @param xml Associated xml.
     * @return Status code.
     * @throws ServiceException service exception
     */
    private int downloadFileByStream(String filename, String inputUrl, String xml) throws ServiceException{
        log.info("------------downloadFileByStream called with " + inputUrl +"--------------");
        String targetLocation = GlobalData.getDownloadDestination(); //So you only have to change it one place instead of two.
        String target = targetLocation+filename+extension;
        log.info("------------target = " + target + "--------------");
        File tmp = null;
        tmp = new File(target);
        if(tmp.exists()){
            log.info("------------Error 409: File already exists. " + target + "--------------");
            return 409; //Error 409 Conflict: File already exists
        }
        HttpClient client = new HttpClient();
        GetMethod method = new GetMethod(inputUrl);
        InputStream reader;
        try{
            client.executeMethod(method);
            reader = method.getResponseBodyAsStream();
        } catch(IOException ex){
            method.releaseConnection(); //Not sure if this is needed here, but just to be sure.
            throw new ServiceException(ex);
        }
        int statusCode = method.getStatusCode();
        if(statusCode == 200){ //StatusCode okay, downloading the file.
            new DownloadService(target, method, reader, xml, targetLocation, filename).start();
        }
        return statusCode;
    }

    /**
     * Finds and returns the YouSee equivalent of the sBChannelId input.
     * @param sBChannelId sbChannelId to be translated.
     * @param date date
     * @return YouSeeChannelId.
     * @throws ServiceException if more than one mapping for sbChannelId is found
     */
    private String getYouSeeChannelId(String sBChannelId, ZonedDateTime date) throws ServiceException{
        List<YouSeeChannelMapping> mappings = null;

        mappings = youSeeChannelMappingDAO.getMappingsFromSbChannelId(sBChannelId, Date.from(date.toInstant()));
        if(mappings.size() == 1){
            return mappings.get(0).getYouSeeChannelId();
        } else {
            throw new ServiceException("Expected a unique mapping for '" + sBChannelId  + "' at "
                    + date + " but found " + mappings.size() + ".");
        }
    }

    private ZonedDateTime convertToUTC(ZonedDateTime date){
        return date.withZoneSameInstant(ZoneId.of("UTC"));

    }

    /**
     * Translates a date to a String that looks like part of the url needed to access the download web page.
     * @param date to be translated.
     * @return String that looks like part of the url needed to access the download web page.
     */
    private String dateToUrlPart(ZonedDateTime date){
        String result = "";
        //Year
        int year = date.getYear();
        result += year;
        //Month
        int month = date.getMonthValue(); //Adjusting for date.getMonth() starting with 0 instead of 1.
        if(month < 10){
            result += "0"; //Range = 1-9, so would give f.ex. 8 instead of 08, so fixing that.
        }
        result += month;
        //Day
        int day = date.getDayOfMonth();
        if(day < 10){
            result += "0"; //Range = 1-9, so would give f.ex. 8 instead of 08, so fixing that.
        }
        result += day + "_";
        //Hour
        int hour = date.getHour();
        if(hour < 10){
            result += "0"; //Range = 1-9 would give f.ex. 8 instead of 08, so fixing that.
        }
        result += hour;
        //Minutes
        int minutes = date.getMinute();
        if(minutes < 10){
            result += "0"; //Range = 1-9 would give f.ex. 8 instead of 08, so fixing that.
        }
        result += minutes;
        //Seconds
        int seconds = date.getSecond();
        if(seconds < 10){
            result += "0"; //Range = 1-9 would give f.ex. 8 instead of 08, so fixing that.
        }
        result += seconds;
        return result;
    }

    /**
     * Initializes and returns a CfuTvDao.
     * @return A initialized CfuTvDao.
     */
    private CfuTvDAO getCfuTvDao() {
        return new CfuTvDAO(CfuTvHibernateUtil.getInitialisedFactory());
    }

    /**
     * Initializes and returns a YouSeeChannelMappingDAO.
     * @return A initialized YouSeeChannelMappingDAO.
     */
    private YouSeeChannelMappingDAO getYouSeeChannelMappingDao() {
        log.info("--------------getYouSeeChannelMappingDao() called----------------");
        return new YouSeeChannelMappingDAO(CfuTvHibernateUtil.getInitialisedFactory());
    }

    /**
     * Initializes and returns a CompositeProgramDAO.
     * @return A initialized CompositeProgramDAO.
     */
    private CompositeProgramDAO getCompositeProgramDao() {
        log.info("--------------getCompositeProgramDao() called----------------");
        return new CompositeProgramDAO(CfuTvHibernateUtil.getInitialisedFactory());
    }
}
