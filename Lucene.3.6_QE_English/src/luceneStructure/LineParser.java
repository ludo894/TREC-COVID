package luceneStructure;

/**
 * LineParser.java
 *
 * Created on June 29, 2020, 10:30 AM
 *
 * @author Ludovic Mourot
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Class parsing the contents of the lines from the queries file
public class LineParser {
	
	//--------------------------------------------------
	////Method analyzing the lines of the document to fill in the information of the query
	//--------------------------------------------------
	public void analyseLine( String line, int [] topicId, String [] searchQuery, boolean [] executeQuery ) {
		//Regular expressions that we want to identify on the line
		String numRegEx = "<num>.*number:\\d+";
		String queryRegEx = "^[aA-zZ].*";
		String finRegEx = "<\\/top>";

		// Create a Pattern object for each regex
		Pattern numPattern = Pattern.compile(numRegEx);
		Pattern queryPattern = Pattern.compile(queryRegEx);
		Pattern finPattern = Pattern.compile(finRegEx);
		
		// Now create the associated matchers objects
		Matcher numMatcher = numPattern.matcher(line);
		Matcher queryMatcher = queryPattern.matcher(line);
		Matcher finMatcher = finPattern.matcher(line);
		if (numMatcher.find()) {
			System.out.println("query num found !");
			
			//Extract the number of the query from the line's string
			char [] chars = numMatcher.group().toCharArray();
			StringBuilder sb = new StringBuilder();
			for(char c : chars){
				if(Character.isDigit(c)){
					sb.append(c);
				}
			}
			topicId[0]= Integer.parseInt(sb.toString());
		} else if (queryMatcher.find()) {
			System.out.println("query found !");
			searchQuery[0] = queryMatcher.group();
		} else if (finMatcher.find()) {
			System.out.println("query end found !");
			executeQuery[0] = true;
		} else {
			System.out.println("NO MATCH");
		}
	}
	
	//--------------------------------------------------
	//Method Main containing the tests of the class
	//--------------------------------------------------
	public static void main(String [] args) {
		/*String line = "<num> number:3";
		int [] topicId = {0};
		String [] searchQuery = {"fail to parse"};
		boolean [] execute = {false};
		LineParser lp = new LineParser();
		
		lp.analyseLine(line, topicId, searchQuery, execute);
		line = "rien ne va plus en Colombie";
		lp.analyseLine(line, topicId, searchQuery, execute);
		line = "</top>";
		lp.analyseLine(line, topicId, searchQuery, execute);
		System.out.println(topicId[0]);
		System.out.println(searchQuery[0]);
		System.out.println(execute[0]);*/
		
	}
}

