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
import org.apache.lucene.queryParser.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class StopWords {

	private static CharArraySet stopSet = CharArraySet.copy( StandardAnalyzer.STOP_WORDS_SET);

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
			System.out.println("Stop words file " + LuceneConstants.STOP_WORDS + " doesn't found");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		StopWords sw = new StopWords();
		sw.init();
		Iterator iterator = sw.stopSet.iterator();
		while (iterator.hasNext()) {
			System.out.print((char[])iterator.next());
			System.out.print(" ");
		}
	}

}
