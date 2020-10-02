package synonymsDB;

import java.sql.*; 
import java.io.File;  // Import the File class
import java.io.FileNotFoundException;  // Import this class to handle errors
import java.io.IOException;
import java.util.Scanner; // Import the Scanner class to read text files

import org.apache.lucene.queryParser.ParseException;

import luceneStructure.LineParser;
import luceneStructure.ResultWriter;

public class InsertSynonyms {

	public static void main(String[] args) {
		try {
			//Connection to the database
			Connection co = DriverManager.getConnection("jdbc:mysql://localhost:3306/synonymsdb?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC","ludo", "treccovid19");
			System.out.println("Successfully connected to the database ");
			Statement st = co.createStatement();

			//Parsing of the datafile
			String dataFileName = "synonyms.txt";
			File datafile = new File(dataFileName); //Queries file to analyze
			Scanner myReader = new Scanner(datafile); //File reader
			System.out.println("reader successfully opened");
			String [] word = {""};
			String [] synonym1 = {""};
			String [] synonym2 = {""};
			DataLineParser dataLineParser = new DataLineParser();

			while(myReader.hasNextLine()) {
				//Retrieve line data
				String line = myReader.nextLine();
				dataLineParser.analyseLine(line, word, synonym1, synonym2);
				
				//Process to the line data insert
				String query = "INSERT INTO synonyms (word, synonym1, synonym2) VALUES (" + "'"+ word[0] + "'" + ", " + "'" +synonym1[0] + "'" + ", " + "'" + synonym2[0] + "'" + ")";
				System.out.println(query);
				st.executeUpdate(query);
			}
			myReader.close();
			st.close();
			co.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			System.out.println("Error during the parsing process");
		}

	}
}
