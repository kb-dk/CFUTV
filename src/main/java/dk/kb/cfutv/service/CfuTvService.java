package dk.kb.cfutv.service;

import dk.kb.cfutv.GlobalData;
import dk.kb.cfutv.model.ReducedRitzauProgram;
import dk.kb.cfutv.persistence.CfuTvDAO;
import dk.kb.cfutv.persistence.CfuTvHibernateUtil;
import dk.kb.cfutv.persistence.CompositeProgramDAO;
import dk.statsbiblioteket.digitaltv.access.model.RitzauProgram;
import dk.statsbiblioteket.mediaplatform.ingest.model.YouSeeChannelMapping;
import dk.statsbiblioteket.mediaplatform.ingest.model.persistence.YouSeeChannelMappingDAO;
import dk.statsbiblioteket.mediaplatform.ingest.model.service.ServiceException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: asj
 * Date: 16-08-12
 * Time: 11:03
 * To change this template use File | Settings | File Templates.
 */
public class CfuTvService {
    private Logger log;
    private CfuTvDAO cfuTvDAO;
    private YouSeeChannelMappingDAO youSeeChannelMappingDAO;
    private CompositeProgramDAO compositeProgramDAO;
    private String baseUrl = GlobalData.getYouSeeAccessUrl();
    private String extension = ".ts"; //extension of downloaded file, .ts chosen as it is also used in preexisting yousee stuff

    public CfuTvService(){
        log = LoggerFactory.getLogger(CfuTvService.class);
        cfuTvDAO = getCfuTvDao();
        youSeeChannelMappingDAO = getYouSeeChannelMappingDao();
        compositeProgramDAO = getCompositeProgramDao();
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
    public List<ReducedRitzauProgram> search(String channel_name, Date from, Date to, String title, String description) {
        List<RitzauProgram> fullPrograms = cfuTvDAO.search(channel_name, from, to, title, description);
        Map<Long, ReducedRitzauProgram> resultMap = new HashMap<Long, ReducedRitzauProgram>();
        for(RitzauProgram rp:fullPrograms){
            if(resultMap.containsKey(rp.getProgram_id())){

                RitzauProgram program1 = cfuTvDAO.getByFullId(resultMap.get(rp.getProgram_id()).getId());
                RitzauProgram program2 = cfuTvDAO.getByFullId(rp.getId());
                boolean tvMeterAvailableForProgram1 = compositeProgramDAO.hasTvMeter(program1);
                boolean tvMeterAvailableForProgram2 = compositeProgramDAO.hasTvMeter(program2);

                if(tvMeterAvailableForProgram1 && tvMeterAvailableForProgram2 || !tvMeterAvailableForProgram1 && !tvMeterAvailableForProgram2){
                    if(program1.getId() < program2.getId()){  //Choosing the one with the highest ID (newest)
                        resultMap.put(rp.getProgram_id(), new ReducedRitzauProgram(rp.getChannel_name(),rp.getId(),rp.getStarttid(),
                                rp.getSluttid(),rp.getTitel(),rp.getKortomtale(), rp.getProgram_id()));
                    }
                }else if(!tvMeterAvailableForProgram1 && tvMeterAvailableForProgram2){
                    resultMap.put(rp.getProgram_id(), new ReducedRitzauProgram(rp.getChannel_name(),rp.getId(),rp.getStarttid(),
                            rp.getSluttid(),rp.getTitel(),rp.getKortomtale(), rp.getProgram_id()));
                }
            }else{
                resultMap.put(rp.getProgram_id(), new ReducedRitzauProgram(rp.getChannel_name(),rp.getId(),rp.getStarttid(),
                        rp.getSluttid(),rp.getTitel(),rp.getKortomtale(), rp.getProgram_id()));
            }
        }
        return new ArrayList<ReducedRitzauProgram>(resultMap.values());
    }

    /**
     * Find a RitzauProgram in the database and uses the PBCoreGenerator to return PBCore xml of found program.
     * @param programId programId of the wanted RitzauProgram.
     * @return full PBCore xml (as defined by the template) of a RitzauProgram.
     * @throws ServiceException if programId is null
     */
    public String getFullPost(Long programId) throws ServiceException{
        if(programId == null){
            throw new ServiceException("ProgramId is null.");
        }
        RitzauProgram program = cfuTvDAO.getByFullId(programId);
        boolean tvMeterAvailable = compositeProgramDAO.hasTvMeter(program);
        PBCoreGenerator generator = new PBCoreGenerator();
        return generator.generateXmlFromTemplate(program,tvMeterAvailable);
    }

    /**
     * Finds and uploads a program, based on id and offsets, to the FtpServer as a file with requested filename, along
     * with PBCore xml about the program.
     * Returns a status code depending on whether it was successful.
     * @param programId Id of wanted program.
     * @param fileName Wanted filename.
     * @param offsetStart Offset from start of the program.
     * @param offsetEnd Offset from end of the program.
     * @return Status code depending on whether it was successful or not.
     * @throws ServiceException service exception
     */
    public int getProgramSnippet(Long programId, String fileName, Date offsetStart, Date offsetEnd) throws ServiceException{
        log.info("----------------getProgramSnippet method called---------------");
        int statusCode;
        RitzauProgram program;

            program = cfuTvDAO.getByFullId(programId);

        if(program == null){
            return -0; //program is null...which error code would that be?
        }
        String sBChannelId = program.getChannel_name();
        String youSeeChannelId = getYouSeeChannelId(sBChannelId, program.getStarttid());
        youSeeChannelId = youSeeChannelId.replaceAll(" ","%20"); //Replace space with http equivalent
        String channelUrlPart = youSeeChannelId + "_"; //ChannelId part of the url.
        Date startTid;
        Date slutTid;
        if(compositeProgramDAO.hasTvMeter(program)){
            startTid = convertToUTC(compositeProgramDAO.getCorrespondingCompositeProgram(program).getTvmeterProgram().getStartDate());
            slutTid = convertToUTC(compositeProgramDAO.getCorrespondingCompositeProgram(program).getTvmeterProgram().getEndDate());
        }else{
            startTid = convertToUTC(program.getStarttid());
            slutTid = convertToUTC(program.getSluttid());
        }
        //offsetting start
        startTid.setSeconds(startTid.getSeconds() - offsetStart.getSeconds());
        startTid.setMinutes(startTid.getMinutes() - offsetStart.getMinutes());
        startTid.setHours(startTid.getHours() - offsetStart.getHours());
        //offsetting end
        slutTid.setSeconds(slutTid.getSeconds() + offsetEnd.getSeconds());
        slutTid.setMinutes(slutTid.getMinutes() + offsetEnd.getMinutes());
        slutTid.setHours(slutTid.getHours() + offsetEnd.getHours());
        //offsetting complete
        String fromUrlPart = dateToUrlPart(startTid) + "_"; //From part of the url.
        String toUrlPart = dateToUrlPart(slutTid) + extension; //To part of the url.
        String downloadUrl = baseUrl + channelUrlPart + fromUrlPart + toUrlPart; //Putting the parts together.
        try {
            String xml = getFullPost(programId);
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
    public int getRawCut(String sBChannelId,String fileName,Date from,Date to) throws ServiceException{
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
        } catch(HttpException ex){
            method.releaseConnection(); //Not sure if this is needed here, but just to be sure.
            throw new ServiceException(ex);
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
    private String getYouSeeChannelId(String sBChannelId, Date date) throws ServiceException{
        List<YouSeeChannelMapping> mappings = null;

        mappings = youSeeChannelMappingDAO.getMappingsFromSbChannelId(sBChannelId, date);
        if(mappings.size() == 1){
            return mappings.get(0).getYouSeeChannelId();
        } else {
            throw new ServiceException("Expected a unique mapping for '" + sBChannelId  + "' at "
                    + date + " but found " + mappings.size() + ".");
        }
    }

    private Date convertToUTC(Date date){
        Locale locale = Locale.getDefault();
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT,locale);

        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String utctime = formatter.format(date);
        Date utcDate = new Date(utctime);
        return utcDate;
    }

    /**
     * Translates a date to a String that looks like part of the url needed to access the download web page.
     * @param date to be translated.
     * @return String that looks like part of the url needed to access the download web page.
     */
    private String dateToUrlPart(Date date){
        String result = "";
        //Year
        int year = date.getYear() + 1900; //Adjust for date.getYears() getting years since 1900.
        result += year;
        //Month
        int month = date.getMonth() + 1; //Adjusting for date.getMonth() starting with 0 instead of 1.
        if(month < 10){
            result += "0"; //Range = 1-9, so would give f.ex. 8 instead of 08, so fixing that.
        }
        result += month;
        //Day
        int day = date.getDate();
        if(day < 10){
            result += "0"; //Range = 1-9, so would give f.ex. 8 instead of 08, so fixing that.
        }
        result += day + "_";
        //Hour
        int hour = date.getHours();
        if(hour < 10){
            result += "0"; //Range = 1-9 would give f.ex. 8 instead of 08, so fixing that.
        }
        result += hour;
        //Minutes
        int minutes = date.getMinutes();
        if(minutes < 10){
            result += "0"; //Range = 1-9 would give f.ex. 8 instead of 08, so fixing that.
        }
        result += minutes;
        //Seconds
        int seconds = date.getSeconds();
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
