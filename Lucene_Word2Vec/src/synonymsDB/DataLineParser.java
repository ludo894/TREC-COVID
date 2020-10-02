package synonymsDB;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataLineParser {

	//--------------------------------------------------
	////Method analyzing the lines of the document to fill in the information of the query
	//--------------------------------------------------
	public void analyseLine( String line, String [] word, String [] synonym1, String [] synonym2 ) {
		//Regular expressions that we want to identify on the line
		String wordRegEx = "[a-z]+";

		// Create a Pattern object for each regex
		Pattern wordPattern = Pattern.compile(wordRegEx);

		// Now create the associated matchers objects
		Matcher wordMatcher = wordPattern.matcher(line);
		int matchCpt = 0;
		while (wordMatcher.find() && matchCpt < 3) {
			if (matchCpt == 0) {
				word[0] = wordMatcher.group(0);
			} else if (matchCpt == 1) {
				synonym1[0] = wordMatcher.group(0);
			} else {
				synonym2[0] = wordMatcher.group(0);
			}
			matchCpt++;
		}
		matchCpt = 0;
	}

	//--------------------------------------------------
	//Method Main containing the tests of the class
	//--------------------------------------------------
	public static void main(String [] args) {
		String line = "pony ponies horse";
		String [] word = {""};
		String [] synonym1 = {""};
		String [] synonym2 = {""};
		DataLineParser dlp = new DataLineParser();
		dlp.analyseLine(line, word, synonym1, synonym2);
		System.out.println(word[0] + " " + synonym1[0] + " " + synonym2[0]);
		String query = "INSERT INTO synonyms VALUES (" + word[0] + ", " + synonym1[0] + ", " + synonym2[0] + ")";
		System.out.println(query);
		
		line = "boat	sea		harbour";
		dlp.analyseLine(line, word, synonym1, synonym2);
		System.out.println(word[0] + " " + synonym1[0] + " " + synonym2[0]);		
		query = "INSERT INTO synonyms VALUES (" + word[0] + ", " + synonym1[0] + ", " + synonym2[0] + ")";
		System.out.println(query);

	}

}
