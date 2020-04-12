package org.jade.fm.supermanager.gameresult.function;


import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;

import org.jade.fm.supermanager.gameresult.entities.GameResult;
import org.jade.fm.supermanager.gameresult.entities.PlayerResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification.S3Entity;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.S3Object;


public class GameResultFileHandler implements RequestHandler<S3Event, Void> {
	

	private AmazonS3 s3Client = null;
	private Context context = null;
	
	public GameResultFileHandler() {
		s3Client = AmazonS3ClientBuilder.standard()
                .withRegion("us-east-2")
                .build();		
		
		context = null;
	}
	
	public GameResultFileHandler(AmazonS3 s3Client, GameResultFileProcessor processor, Context context) {
        this.s3Client = s3Client;
        this.context = context;
	}
	

    /**
     * This handler is triggered by an S3-put-object event, and then calls AWS Rekognition to tag the
     * newly created S3 object.
     */
    @Override
    public Void handleRequest(S3Event input, Context context) {
    	this.context = context;
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
	            
	            processGameResultFile( gameResultFile );
	            
            }
        }catch (AmazonServiceException e) {
        	context.getLogger().log("Impossible to process file. An error occured. " + e.toString());
        	e.printStackTrace();
        }catch ( GameResultServiceException ex) {
        	context.getLogger().log("Impossible to process file. An error occured. " + ex.toString());
        	ex.printStackTrace();
        }

        return null;
    }

    private void processGameResultFile(S3Object gameResultFile) throws GameResultServiceException{
    	if( gameResultFile == null ) context.getLogger().log("ERROR: impossible to get the game result file");
    	context.getLogger().log("Start processing game result file");
    	
    	try {
    		
    		String htmlFile = gameResultFile.getObjectContent().toString();
    		
    		if( htmlFile == "" ) {
    			context.getLogger().log("Impossbile to process the game file. the file is empty");
    			throw new GameResultServiceException("Impossbile to process the game file. the file is empty");
    		}
    		
    		GameResult gameResult = processFile( htmlFile);
    		getGameresultInfo(gameResult, gameResultFile);
    		
    		storeFile(gameResult);
    		
    	}catch( Exception e) {
    		context.getLogger().log("Impossbile to process the game file. An error occured");
    		throw e;    		
    	}
    }
    
    private void getGameresultInfo( GameResult gameResult, S3Object gameResultFile) {
    	    	
    	String bucketName = gameResultFile.getBucketName();
    	String key = gameResultFile.getKey();
    	
    	key.replace("gamefiles-", "");
    	int index = key.indexOf("-", 0);
    	String homeTeam = key.substring(0, index-1);
    	String awayTeam = key.substring(index + 1, key.length());
    	
    	gameResult.setAwayTeam(awayTeam);
		gameResult.setHomeTeam(homeTeam);
		gameResult.setDate(bucketName);
    }
    
	private GameResult processFile(String fmFile) {
		if(fmFile == "" ) context.getLogger().log( "file not found or input is not a file" );
		
		try {
						
			//check if FMFile
			Document doc = Jsoup.parse(fmFile);
			if(doc == null ) context.getLogger().log("impossible to load the FMFile");
			String eval = "a[href$=http://www.sigames.com/]";
			Elements elements = doc.select(eval);
			if( elements == null || elements.size() != 1) return null;

			//Process file
			String evalAllPlayers = "tr";//TODO
			Elements players = doc.select(evalAllPlayers);
			if( players == null || players.size() == 0 ) return null;
			
			GameResult gameResult = transform(players);
			
			return gameResult;
			
		} catch (Exception e) {
			context.getLogger().log("Impossible to process the file. an error occured");
			context.getLogger().log(e.toString());
			return null;
		}
	}
	

	private GameResult transform( Elements players) {
		if( players == null || players.size() == 0 ) return null;
		
		try {
			String key = "";
			GameResult gameResult = new GameResult();
			
			Map<String, PlayerResult> playerResults = new HashMap<String, PlayerResult>();
			
			
			for (org.jsoup.nodes.Element player : players) {
				if( player == null) continue;
				
				Elements playerAtts = player.getAllElements();
				PlayerResult playerResult = new PlayerResult();
				
				playerResult = populatePlayerResult( playerAtts);
				if( playerResult == null ) continue;
				key = playerResult.getName();
				playerResults.put(key, playerResult);
			}
			
			gameResult.setPlayerResults(playerResults);
			
			return gameResult;
			
		}catch( Exception e) {
			context.getLogger().log("Impossible to transform the file");
			context.getLogger().log(e.toString());
			return null;
		}
	}

	private PlayerResult populatePlayerResult( Elements playerAtts) {
		if( playerAtts == null ) return null;
		
		PlayerResult playerResult = new PlayerResult();
		
		for (org.jsoup.nodes.Element playerAtt : playerAtts) {

			int index = playerAtt.elementSiblingIndex();
			if( index == 1 ) {
				playerResult.setNumero(Integer.parseInt(playerAtt.val()));
				continue;
			}
			if( index == 2 || index == 5 || index == 6 || index == 7 || index == 8) {
				continue;
			}
			if( index == 3 ) {
				playerResult.setName(playerAtt.val());
				continue;
			}
			if( index == 4 ) {
				playerResult.setPosition(playerAtt.val());
				continue;
			}
			if( index == 9 ) {
				int goal = 0;
				if( playerAtt.val() != "" && playerAtt.val() != "-") {
					goal = Integer.parseInt(playerAtt.val());
				}
				playerResult.setGoal(goal);
				continue;
			}
			if( index == 10 ) {
				int mark = 0;
				playerResult.setDidHePlay(false);
				
				if( playerAtt.val() != "" && playerAtt.val() != "-") {
					mark = Integer.parseInt(playerAtt.val());
					playerResult.setDidHePlay(true);
				}
	
				playerResult.setGoal(mark);
				continue;
			}
		}
		return playerResult;
	}

    private void storeFile( GameResult gameResult) throws GameResultServiceException{
    	try {
    		
    	}catch( Exception e) {
    		context.getLogger().log( "Impossible to store the results in the DDB" );
    		throw e;
    	}
    }
    
}
