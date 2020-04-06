package org.jade.fm.supermanager.gameresult.function;

import static org.junit.Assert.*;

import java.awt.List;

import org.junit.Test;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;


public class GameResultFileHandlerTest extends GameResultFileHandler {
	private GameResultFileHandler handler = null;
	private AmazonS3 s3Client =null;
	private String region = "";
	private ProfileCredentialsProvider credentialsProvider = null;
	
	public  GameResultFileHandlerTest() {
		region = "";
		
		ProfileCredentialsProvider credentialsProvider;

        try {
        	credentialsProvider = new ProfileCredentialsProvider("default");
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (C:\\Users\\david\\.aws\\credentials), and is in valid format.",
                    e);
        }

		s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();        
        
	}
	
	@Test
	public void testGameResultFileHandler() {
		S3Event s3Event = null;
		Context context = null;
		
		handler = new GameResultFileHandler(s3Client);
		assertNotNull(handler);
		
		//S3Entity s3 = new 
		//S3EventNotificationRecord record1 = new S3EventNotificationRecord( region, "test-eventName", "s3", "1.5", "test-eventVersion",null, null, s3, user, null);
		
		//List<S3EventNotificationRecord> records = new List();
		
		//S3Event s3Event = new S3Event(records);
		
		handler.handleRequest(s3Event, context);
		
	}

	@Test
	public void testGameResultFileHandlerAmazonS3AWSCredentialsString() {
		fail("Not yet implemented");
	}

	@Test
	public void testHandleRequest() {
		fail("Not yet implemented");
	}

}
