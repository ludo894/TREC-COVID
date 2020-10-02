package luceneStructure;

/**
 * ResultWriter.java
 *
 * Created on June 29, 2020, 10:30 AM
 *
 * @author Ludovic Mourot
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

//Class generating the queries results file in TREC format
public class ResultWriter {
	private String indexDir = LuceneConstants.INDEX_DIR; //Index repository
	private String fileTitle;

	//--------------------------------------------------
	//Method performing a standard query and write its results in an output file
	//--------------------------------------------------
	public void search(int topicId, String searchQuery) throws IOException, ParseException {
		int i = 1;// Document's rank for the topic
		Searcher searcher = new Searcher(indexDir);
		long startTime = System.currentTimeMillis();
		TopDocs hits = searcher.search(searchQuery);//Hits contains the result of the search query
		long endTime = System.currentTimeMillis();
		FileWriter fileWriter = new FileWriter("results_lucene_standard.txt",true); //FileWriter takes in parameter a file where it will write  (typewriter)
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter); //bufferedWriter tells the Filewriter what to write to the file (writer)


		System.out.println(hits.totalHits + " documents found. Time :" + (endTime - startTime));
		for(ScoreDoc scoreDoc : hits.scoreDocs) { //For each document with a non-zero score 'scoreDoc' in the result 'hits'.
			Document doc = searcher.getDocument(scoreDoc);

			//Deleting the .txt extension
			fileTitle = doc.get(LuceneConstants.FILE_NAME).substring(0, doc.get(LuceneConstants.FILE_NAME).lastIndexOf('.'));

			//Standard Output Request Result
			System.out.println(topicId + "\t "+ "QO" + "\t"+ fileTitle + "\t" + i + "\t" +scoreDoc.score + "\t" + "STANDARD");

			//Writing each result line in the ordered list file (output file)
			bufferedWriter.write(topicId + "\t "+ "QO" + "\t"+ fileTitle + "\t" + i + "\t" +scoreDoc.score + "\t" + "STANDARD\n");
			
			//Upadate the document rank indicator
			i++;
		}
		i = 1;
		bufferedWriter.close();
		searcher.close();
	}

	//--------------------------------------------------
	//Method performing an expanded query and write its results in an output file
	//--------------------------------------------------
	public void queryExpandSearch(int topicId, String searchQuery) throws IOException, ParseException {
		//Initializing the FileWritter and Buffered
		FileWriter fileWriter = new FileWriter("results_lucene_expanded.txt",true); //FileWriter takes in parameter a file where it will write  (typewriter)
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter); //bufferedWriter tells the Filewriter what to write to the file (writer)
		
		//Initializing a query expander
		Properties prop = new Properties();
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);
		Directory indexDirectory = FSDirectory.open(new File(indexDir));
		org.apache.lucene.search.Searcher searcher = new IndexSearcher(indexDirectory);
		Similarity similarity = new DefaultSimilarity();
		WordEmbedding queryExpander = new WordEmbedding(analyzer, searcher, similarity, prop);

		//Load the properties of the QueryExpander from the data in the file queryExpansion.properties
		FileReader propertiesReader = new FileReader("queryExpansion.properties");
		prop.load(propertiesReader);

		//Perform the initial query
		QueryParser queryParser = new QueryParser(Version.LUCENE_36,LuceneConstants.CONTENTS, analyzer);
		Query query = queryParser.parse(searchQuery); //Transformation of the user query into a weighted system query
		long startTime = System.currentTimeMillis(); //Get the search process start time
		TopDocs topDocs = searcher.search(query, LuceneConstants.MAX_SEARCH);
		
		/**
		 * Debugging option : Write on the output file the original query
		 */
		//bufferedWriter.write("Topic id: " + topicId +"\n" + "Original query: " + query.toString() + "\n");

		//Store the results of the initial query in a documents vector
		Vector <Document> hits = new Vector();
		for(ScoreDoc scoreDoc : topDocs.scoreDocs) {
			Document doc = searcher.doc(scoreDoc.doc);
			hits.add(doc);
		}

		//Extension of the request
		Query queryExtend = queryExpander.wordEmbeddingQuery(searchQuery, prop, LuceneConstants.FIXED_SYNONYMS_NB);
		/**
		 * Debugging
		 */
		//System.out.println(queryExtend.toString());
		
		/**
		 * Debugging option : Write on the output file the expanded query
		 */
		//bufferedWriter.write("Extended query: " + queryExtend.toString() + "\n");
		
		//Starting the search with the extended query
		topDocs = searcher.search(queryExtend, LuceneConstants.MAX_SEARCH);
		long endTime = System.currentTimeMillis(); //Get the search process end time
		
		//Print expanded query process performance
		System.out.println(topDocs.totalHits + " documents found. Time :" + (endTime - startTime));
		
		//Return of the request
		int i = 1;// Document's rank for the topic
		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			Document doc = searcher.doc(scoreDoc.doc);

			//Deleting the .txt extension
			fileTitle = doc.get(LuceneConstants.FILE_NAME).substring(0, doc.get(LuceneConstants.FILE_NAME).lastIndexOf('.'));

			//Print the result in the standard output
			System.out.println(topicId + "\t "+ "QO" + "\t"+ fileTitle + "\t" + i + "\t" +scoreDoc.score + "\t" + "EXPANDED");
			
			//Writing each result line in the ordered list file (output file)
			bufferedWriter.write(topicId + "\t "+ "QO" + "\t"+ fileTitle + "\t" + i + "\t" +scoreDoc.score + "\t" + "EXPANDED\n");
			
			//Upadate the document rank indicator
			i++;
		}
		i = 1;
		bufferedWriter.close();
		searcher.close();
	}

}
