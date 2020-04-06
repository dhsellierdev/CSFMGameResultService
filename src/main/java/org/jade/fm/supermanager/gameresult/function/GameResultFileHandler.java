package org.jade.fm.supermanager.gameresult.function;


import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification.S3Entity;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;



public class GameResultFileHandler implements RequestHandler<S3Event, Void> {
	

	private AmazonS3 s3Client =null;
	
	public GameResultFileHandler() {
		s3Client = AmazonS3ClientBuilder.standard()
                .withRegion("us-east-2")
                .build();		
	}
	
	public GameResultFileHandler(AmazonS3 s3Client) {
        this.s3Client = s3Client;
	}
	

    /**
     * This handler is triggered by an S3-put-object event, and then calls AWS Rekognition to tag the
     * newly created S3 object.
     */
    @Override
    public Void handleRequest(S3Event input, Context context) {
        context.getLogger().log("Input: " + input);

        try {
	        // For every S3 object
	        for (S3EventNotificationRecord event : input.getRecords()) {
	            S3Entity entity = event.getS3();
	            String bucketName = entity.getBucket().getName();
	            String key = entity.getObject().getUrlDecodedKey();
	            
	            context.getLogger().log("S3 bucket info: Bucket = " + bucketName + ", Key = " + key);
	            if( s3Client == null ) {
	            	context.getLogger().log("s3Client not assigned");
	            	return null;
	            }
	    		
            	S3Object gameResultFile = s3Client.getObject(bucketName, key);
	            if( gameResultFile == null ) context.getLogger().log("ERROR: impossible to get the game result file");
	            processGameResultFile( gameResultFile, context );
            }
        }catch (AmazonServiceException e) {
                e.printStackTrace();
        }

        return null;
    }
    
    private void processGameResultFile(S3Object gameResultFile, Context context) {
    	if( gameResultFile == null ) context.getLogger().log("ERROR: impossible to get the game result file");
    	context.getLogger().log("Start processing game result file");
    	
    	
    }
    
}
