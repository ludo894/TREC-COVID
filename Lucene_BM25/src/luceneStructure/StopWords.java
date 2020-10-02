package luceneStructure;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class StopWords {

	private static CharArraySet stopSet = new CharArraySet(1, true);

	public static void init() {
		try {
			File stopWordFile = new File(LuceneConstants.STOP_WORDS); //Queries file to analyze
			Scanner myReader = new Scanner(stopWordFile); //File reader
			String line;
			while (myReader.hasNextLine()) {
				line = myReader.nextLine();
				stopSet.add(line);
			}
			myReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("Stop words file " + LuceneConstants.STOP_WORDS + " not found");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public CharArraySet getStopSet() {
		return stopSet;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		StopWords sw = new StopWords();
		sw.init();
		Iterator iterator = sw.stopSet.iterator();
		while (iterator.hasNext()) {
			System.out.println((char[])iterator.next());
		}
	}

}
