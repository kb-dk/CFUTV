package dk.kb.cfutv.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.cfutv.GlobalData;
import dk.kb.cfutv.model.ReducedRitzauProgram;
import dk.kb.cfutv.service.CfuTvService;
import dk.statsbiblioteket.mediaplatform.ingest.model.service.ServiceException;

@Path("/")
public class CfuTvServlet {
    
    private Logger log = LoggerFactory.getLogger(getClass());
    private static CfuTvService service;

    public CfuTvServlet() {
        service = new CfuTvService();
    }

	/**
	 * Searches the db using input and returns xml of the resulting list.
	 * Each result has channel name, programId, startTime, endTime, title and a short description.
     * @param channel_name name for channel (mapped)
     * @param fromInput date
     * @param toInput date
     * @param title titel
     * @param description Kortomtale, langomtale1 eller langomtale2.
	 * @return xml containing a list of results.
	 */
	@GET
	@Path("search")
	@Produces(MediaType.APPLICATION_XML)
	public List<ReducedRitzauProgram> search(@QueryParam("channelName") String channel_name,
			@QueryParam("from") String fromInput,
			@QueryParam("to") String toInput,
			@QueryParam("title") String title,
			@QueryParam("description") String description){
		ZonedDateTime from = null;
		ZonedDateTime to = null;
		DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm", Locale.ROOT).withZone(ZoneId.of("Europe/Copenhagen"));
		if(fromInput != null && fromInput.trim().length() > 0){

			try{
				from = ZonedDateTime.parse(fromInput, format);
			} catch(DateTimeParseException ex){
				log.debug("Date parse error: From: " + fromInput);
				log.debug("Date parse error stacktrace: " + ex.getStackTrace());
				String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
				result += "<error code=\"400\">Bad Request: From could not be parsed. Use following format: yyyy-MM-dd_HH:mm</error>";
				throw new WebApplicationException(Response.status(400).entity(result).build());
			}
		}

		if(toInput != null && toInput.trim().length() > 0){
			try{
				to = ZonedDateTime.parse(toInput, format);
			} catch(DateTimeParseException ex){
				log.debug("Date parse error: To: " + toInput);
				log.debug("Date parse error stacktrace: " + ex.getStackTrace());
				String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
				result += "<error code=\"400\">Bad Request: To could not be parsed. Use following format: yyyy-MM-dd_HH:mm</error>";
				throw new WebApplicationException(Response.status(400).entity(result).build());
			}
		}

		if(from != null && to!=null){
			if(from.compareTo(to) > 0){
				String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
				result += "<error code=\"400\">Bad Request: From is not before To.</error>";
				throw new WebApplicationException(Response.status(400).entity(result).build());
			}
		}

		return service.search(channel_name,from,to,title,description);
	}

	/**
	 * Finds and returns PBCore xml metadata for a single programId.
	 * @param IdRaw program id
	 * @return PBCore xml.
	 */
	@GET
	@Path("fullpost")
	@Produces(MediaType.APPLICATION_XML)
	public Response fullPost(@QueryParam("id") String IdRaw){
		if(IdRaw == null || IdRaw.trim().length() == 0){
			String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			result += "<error code=\"400\">Bad Request: No id.</error>";
			return Response.status(400).entity(result).build();
		}
		try{
			Long Id = Long.parseLong(IdRaw);
			try{
				String result = service.getFullPost(Id);
				log.info("----------------------FULLPOST SUCCESS--------------------");
				if(result == null || result.trim().length() == 0){
					result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
					result += "<error code=\"410\">No program found with that id.</error>";
					return Response.status(410).entity(result).build();
				}
				return Response.status(200).entity(result).build();
			} catch(ServiceException ex){
				throw new WebApplicationException(ex,Response.Status.INTERNAL_SERVER_ERROR);
			}
		} catch(NumberFormatException ex){
			String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			result += "<error code=\"400\">Bad Request: Id can only be numbers.</error>";
			return Response.status(400).entity(result).build();
		}
	}

	/**
	 * Finds and uploads a program, based on id and offsets, to the FtpServer as a file with requested filename, along
	 * with PBCore xml about the program.
	 * Sends back a response with a status code based on whether or not it was successful.
	 * Status codes:
	 * 200: OK.
	 * 400: Bad information in url.
	 * 404: Content not available in main archive. Try again later.
	 * 409: A file with chosen filename already exists on FtpServer.
	 * 410: Content not available.
	 * 500: Internal server error or unexpected status code.
	 * @param IdRaw Id of the program.
	 * @param filename Wanted filename.
	 * @param offsetStartRaw Offset from start of the program in HH:mm:ss format.
	 * @param offsetEndRaw Offset from end of the program in HH:mm:ss format.
	 * @return Status code in Response and requested file and associated PBCore xml on FtpServer.
	 */
	@GET
	@Path("programSnippet")
	@Produces(MediaType.TEXT_PLAIN)
	public Response programSnippet(@QueryParam("id") String IdRaw,
			@QueryParam("filename") String filename,
			@QueryParam("offsetStart") String offsetStartRaw,
			@QueryParam("offsetEnd") String offsetEndRaw){
		if(IdRaw == null || IdRaw.trim().length() == 0 || filename == null || filename.trim().length() == 0){
			String text = "Bad information in url. Make sure to set id, filename and offsets.";
			log.info("-----------programSnippet first opportunity exit with 400---------------");
			return Response.status(400).entity(text).build();
		}
		ZonedDateTime offsetStart = null;
		ZonedDateTime offsetEnd = null;
		if(offsetStartRaw != null && offsetStartRaw.trim().length() > 0 && offsetEndRaw != null && offsetEndRaw.trim().length() > 0) {
			DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT).withZone(ZoneId.of("Europe/Copenhagen"));


			try{
				offsetStart = ZonedDateTime.parse(offsetStartRaw, format);
				offsetEnd = ZonedDateTime.parse(offsetEndRaw, format);
			} catch(DateTimeParseException ex) {
				log.debug("Date parse error: From: " + offsetStartRaw + " | To: " + offsetEndRaw);
				log.debug("Date parse error stacktrace: " + ex.getStackTrace());
				String text = "Failed to parse dates, make sure it is of following format: yyyy-MM-dd_HH:mm:ss";
				return Response.status(400).entity(text).build();
			}
		} else {
			String text = "Bad information in url. Make sure to set id, filename and offsets.";
			log.info("-----------programSnippet second opportunity exit with 400---------------");
			return Response.status(400).entity(text).build();
		}
		try{
			Long Id = Long.parseLong(IdRaw);
			try{
				int statusCode = service.getProgramSnippet(Id, filename, offsetStart, offsetEnd);
				if(statusCode == 200){
					String text = "OK";
					log.info("-----------programSnippet exit with 200---------------" + statusCode);
					return Response.status(statusCode).entity(text).build();
				}
				if(statusCode == 400){
					String text = "Bad information in url (for instance, unknown channel id)";
					log.info("-----------programSnippet exit with 400---------------" + statusCode);
					return Response.status(statusCode).entity(text).build();
				}
				if(statusCode == 404){
					String text = "Content unavailable on primary archive server, but exists on secondary. Try later.";
					log.info("-----------programSnippet exit with 404---------------" + statusCode);
					return Response.status(statusCode).entity(text).build();
				}
				if(statusCode == 409){
					String text = "A file with that filename already exists.";
					log.info("-----------programSnippet exit with 409---------------" + statusCode);
					return Response.status(statusCode).entity(text).build();
				}
				if(statusCode == 410){
					String text = "Content unavailable, request cannot be fulfilled.";
					log.info("-----------programSnippet exit with 410---------------" + statusCode);
					return Response.status(statusCode).entity(text).build();
				}
				if(statusCode == -0){
					String text = "Program id not found.";
					log.info("-----------programSnippet exit with 410---------------" + statusCode);
					return Response.status(410).entity(text).build();
				}
				log.info("-----------programSnippet exit with 500, unexpected status code---------------" + statusCode);
				return Response.status(500).entity("Unexpected status code.").build(); //Unexpected status code
			} catch(ServiceException ex){
				log.info("-----------programSnippet exit with 500, internal server---------------" + ex.getMessage(), ex);
				return Response.status(500).entity("Internal server error.").build();
			}
		} catch(NumberFormatException ex){
			log.info("-----------programSnippet exit with 400, invalid program id---------------");
			return Response.status(400).entity("Invalid program id, must only contain numbers.").build();
		}
	}

	/**
	 * Finds and uploads interval on a channel, based on channel and interval, to the FtpServer as a file with
	 * requested filename.
	 * Sends back a response with a status code based on whether or not it was successful.
	 * Status codes:
	 * 200: OK.
	 * 400: Bad information in url.
	 * 404: Content not available in main archive. Try again later.
	 * 409: A file with chosen filename already exists on FtpServer.
	 * 410: Content not available.
	 * 500: Internal server error or unexpected status code.
	 * @param channel Which channel
	 * @param filename Wanted filename
	 * @param fromRaw from when, format: yyyy-MM-dd_HH:mm:ss
	 * @param toRaw to when, format: yyyy-MM-dd_HH:mm:ss
	 * @return Status code in Response and requested file on FtpServer.
	 */
	@GET
	@Path("rawCut")
	@Produces(MediaType.TEXT_PLAIN)
	public Response rawCut(@QueryParam("channel") String channel,
			@QueryParam("filename") String filename,
			@QueryParam("from") String fromRaw,
			@QueryParam("to") String toRaw){
		if(channel == null || channel.trim().length() == 0 || filename == null || filename.trim().length() == 0){
			String text = "Bad information in url. Make sure to set channel, filename, from and to.";
			log.info("-----------rawCut first opportunity exit with 400---------------");
			return Response.status(400).entity(text).build();
		}
		ZonedDateTime from = null;
		ZonedDateTime to = null;
		if(fromRaw != null && fromRaw.trim().length() > 0 && toRaw != null && toRaw.trim().length() > 0){
			DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss", Locale.ROOT).withZone(ZoneId.of("Europe/Copenhagen"));
			try{
				from = ZonedDateTime.parse(fromRaw, format);
				to = ZonedDateTime.parse(toRaw, format);
			} catch(DateTimeParseException ex) {
				log.debug("Date parse error: From: " + fromRaw + " | To: " + toRaw);
				log.debug("Date parse error stacktrace: " + ex.getStackTrace());
				String text = "Failed to parse dates, make sure it is of following format: yyyy-MM-dd_HH:mm:ss";
				return Response.status(400).entity(text).build();
			}
		} else {
			String text = "Bad information in url. Make sure to set channel, filename, from and to.";
			log.info("-----------rawCut second opportunity exit with 400---------------");
			return Response.status(400).entity(text).build();
		}
		if(from.isAfter(to)){
			return Response.status(400).entity("From must be before To.").build();
		}
		try{
			int statusCode = service.getRawCut(channel,filename,from,to);
			if(statusCode == 200){
				String text = "OK";
				log.info("-----------rawCut exit with 200---------------" + statusCode);
				return Response.status(statusCode).entity(text).build();
			}
			if(statusCode == 400){
				String text = "Bad information in url (for instance, unknown channel id)";
				log.info("-----------rawCut exit with 400---------------" + statusCode);
				return Response.status(statusCode).entity(text).build();
			}
			if(statusCode == 404){
				String text = "Content unavailable on primary archive server, but exists on secondary. Try later.";
				log.info("-----------rawCut exit with 404---------------" + statusCode);
				return Response.status(statusCode).entity(text).build();
			}
			if(statusCode == 409){
				String text = "A file with that filename already exists.";
				log.info("-----------rawCut exit with 409---------------" + statusCode);
				return Response.status(statusCode).entity(text).build();
			}
			if(statusCode == 410){
				String text = "Content unavailable, request cannot be fulfilled.";
				log.info("-----------rawCut exit with 410---------------" + statusCode);
				return Response.status(statusCode).entity(text).build();
			}
			log.info("-----------rawCut exit with 500, unexpected status code---------------" + statusCode);
			return Response.status(500).entity("Unexpected status code.").build(); //Unexpected status code
		} catch(ServiceException ex){
			log.info("-----------rawCut exit with 500, internal server---------------" + ex.getMessage(), ex);
			return Response.status(500).entity("Internal server error.").build();
		}
	}

	/**
	 * Returns xml with filename, size and last modification date of file(s) found.
	 * Finds specific file if given a filename. If not then it finds all files at download destination.
	 * @param filename wanted file, remember extensions.
	 * @return Xml with filename, size and last modification date of file(s) found.
	 */
	@GET
	@Path("status")
	@Produces(MediaType.APPLICATION_XML)
	public Response getStatus(@QueryParam("filename") String filename){
		String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		if(filename == null || filename.trim().length() == 0){
			ArrayList<File> files = service.getStatusAll();
			result += "<files>";
			for(File f : files){
				filename = f.getName();
				double size = ((double) f.length()) / 1024;
				long datetime = f.lastModified();
				ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochMilli(datetime), ZoneId.of("Europe/Copenhagen"));
				DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT).withZone(ZoneId.of("Europe/Copenhagen"));
				String lastModified = format.format(date);
				result += "<file size=\"" + size + " KB\" lastModified=\"" + lastModified + "\">"+filename+"</file>";
			}
			result += "</files>";
			return Response.status(200).entity(result).build();
		} else {
			File f = service.getStatus(filename);
			if(!f.exists()){
				result += "<error code=\"400\">File not found: " + filename + "</error>";
				return Response.status(400).entity(result).build();
			}
			double size = ((double) f.length()) / 1024;
			long datetime = f.lastModified();
			ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochMilli(datetime), ZoneId.of("Europe/Copenhagen"));
			DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT).withZone(ZoneId.of("Europe/Copenhagen"));
			String lastModified = format.format(date);
			result += "<file size=\"" + size + " KB\" lastModified=\"" + lastModified + "\">"+filename+"</file>";
			return Response.status(200).entity(result).build();
		}
	}

	/**
	 * Finds specific file given a filename.
	 * @param fileName wanted file, remember extensions.
	 * @param response servlet response
	 * @return Output stream with file
	 */
	@GET
	@Path("fileDownload")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getFileFromServer(@QueryParam("filename") String fileName, @Context HttpServletResponse response){
		ServletOutputStream out = null;
		InputStream in = null;
		File file = new File(GlobalData.getDownloadDestination()+fileName);
		byte[] bytes = new byte[4 * 1024]; //Buffer size, should be what?
		int bytesRead;
		response.addHeader("Content-Disposition","attachment; filename=\"" + fileName + "\"");
		
		if(fileName.substring(fileName.lastIndexOf(".")+1,fileName.length()).equals("ts")){
			response.addHeader("Content-Type", "video/MP2T");
		}else if(fileName.substring(fileName.lastIndexOf(".")+1,fileName.length()).equals("xml")){
			response.addHeader("Content-Type", "application/xml");
		}
		
		response.addHeader("Content-Length", String.valueOf(file.length()));
		
		try {
			out = response.getOutputStream();
			in = new FileInputStream(file);
			while ((bytesRead = in.read(bytes)) != -1) {
				out.write(bytes, 0, bytesRead);
			}
			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			String text = "File with name "+fileName+" cannot be found.";
			return Response.status(400).entity(text).build();
		} catch (IOException e){
			return Response.status(500).entity("Internal server error.").build();
		}
		
		return Response.status(200).build();
	}
	
	/**
	 * Returns current date and time.
	 * @return current date and time.
	 */
	@GET
	@Path("test")
	@Produces(MediaType.APPLICATION_XML)
	public Response getTest(){
		String output = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		output += "<test><methodCalledTime>" + new Date() + "</methodCalledTime>";
		output += "</test>";
		log.info("getTest: " + output);
		return Response.status(200).entity(output).build();
	}
}
