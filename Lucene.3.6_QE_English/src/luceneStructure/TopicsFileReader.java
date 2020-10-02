package luceneStructure;

/**
 * TopicsFileReader.java
 *
 * Created on June 29, 2020, 10:30 AM
 *
 * @author Ludovic Mourot
 */

import java.io.File;  // Import the File class
import java.io.FileNotFoundException;  // Import this class to handle errors
import java.io.IOException;
import java.util.Scanner; // Import the Scanner class to read text files

import org.apache.lucene.queryParser.ParseException;

//Class reading the TREC request document
public class TopicsFileReader {
	
	private  int [] topicId = {0};
	private String [] searchQuery = {""};
	private boolean [] executeQuery = {false};
	private String line = "";
	private boolean queryExpansion;
	
	//Getters
	public int [] getTopicId() {
		return topicId;
	}

	public String [] getSearchQuery() {
		return searchQuery;
	}
	
	public TopicsFileReader(boolean queryExpansion) {
		this.queryExpansion = queryExpansion;
	}
	
	//--------------------------------------------------
	//Method reading the queries file
	//--------------------------------------------------
	public void readFile(String topicsFileName) {
		try {
			File topicsFile = new File(topicsFileName); //Queries file to analyze
			Scanner myReader = new Scanner(topicsFile); //File reader
			LineParser lineParser = new LineParser();	//Line Content Analyzer
			ResultWriter resultWriter = new ResultWriter(); //Writer: writes the queries results file

			while (myReader.hasNextLine()) {
				line = myReader.nextLine();
				lineParser.analyseLine(line, topicId, searchQuery, executeQuery);
				//If all the information in the query has been retrieved, the search is launched
				if (executeQuery[0] == true && queryExpansion == false) {
					resultWriter.search(topicId[0], searchQuery[0]);
					executeQuery[0] = false;
				} else if (executeQuery[0] == true && queryExpansion == true) {
					resultWriter.queryExpandSearch(topicId[0], searchQuery[0]);
					executeQuery[0] = false;
				}
			}
			myReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}
