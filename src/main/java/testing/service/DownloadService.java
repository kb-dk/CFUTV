package testing.service;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadService extends Thread{
	
	private String target;
	private GetMethod method;
	private InputStream reader;
	private String xml;
	private String targetLocation;
	private String filename;
	private Logger log;
	
	public DownloadService(String target, GetMethod method, InputStream reader, String xml, String targetLocation, String filename){
		this.target = target;
		this.method = method;
		this.reader = reader;
		this.xml = xml;
		this.targetLocation = targetLocation;
		this.filename = filename;
		log = LoggerFactory.getLogger(CfuTvService.class);
	}
	
	public void run(){
		FileOutputStream writer = null;
	     try {
	         writer = new FileOutputStream(target);
	     } catch(FileNotFoundException ex){
	         //Clean up connection resources as the code won't proceed to where they are needed if it ends up here.
	         method.releaseConnection();
	         log.error(ex.getMessage());
	     }
	     byte[] buffer = new byte[153600];
	     int bytesRead;

	     try{
	         while((bytesRead = reader.read(buffer)) > 0){
	             writer.write(buffer,0,bytesRead);
	             buffer = new byte[153600];
	         }
	     } catch (IOException ex){
	    	 log.error(ex.getMessage());
	     } finally {
	         try{
	             writer.close();
	             reader.close();
	         } catch(IOException ex){
	        	 log.error(ex.getMessage());
	         } finally {
	             method.releaseConnection(); //Clean up connection resources as they are no longer needed.
	         }
	     }
	     if(xml != null){
	         try{
	             writer = new FileOutputStream(targetLocation+filename+".xml");
	             buffer = xml.getBytes(Charset.forName("UTF-8"));
	             writer.write(buffer,0,buffer.length);
	             writer.close();
	         } catch (FileNotFoundException ex){
	        	 log.error(ex.getMessage());
	         } catch(IOException ex){
	        	 log.error(ex.getMessage());
	         }
	     }
	}
}
