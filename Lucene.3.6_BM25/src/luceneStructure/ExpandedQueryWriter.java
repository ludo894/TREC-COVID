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

//Class generating the expanded queries use by the BM25 system
public class ExpandedQueryWriter {
	private String indexDir = LuceneConstants.INDEX_DIR; //Index repository
	private String fileTitle;

	//--------------------------------------------------
	//Method writing an expanded query for BM25 in an output file
	//--------------------------------------------------
	public void queryExpandSearch(int topicId, String searchQuery) throws IOException, ParseException {
		//Initializing the FileWritter and Buffered
		FileWriter fileWriter = new FileWriter("expanded_quries.txt",true); //FileWriter takes in parameter a file where it will write  (typewriter)
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter); //bufferedWriter tells the Filewriter what to write to the file (writer)

		//Initializing a query expander
		Properties prop = new Properties();
		StopWords stopWords = new StopWords();
		stopWords.init();
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_36, stopWords.getStopSet());
		Directory indexDirectory = FSDirectory.open(new File(indexDir));
		org.apache.lucene.search.Searcher searcher = new IndexSearcher(indexDirectory);
		Similarity similarity = new DefaultSimilarity();
		QueryExpansion queryExpander = new QueryExpansion(analyzer, searcher, similarity, prop);

		//Load the properties of the QueryExpander from the data in the file queryExpansion.properties
		FileReader propertiesReader = new FileReader("queryExpansion.properties");
		prop.load(propertiesReader);

		//Fill the hits vector with the extension terms from the selected relevant documents
		TopDocs topDocs;
		String [] relevantDocs = ReturnedDocs.RETURNED_DOCS[topicId-1];
		long startTime = System.currentTimeMillis(); //Get the search process start time

		//Store the terms of the selected relevant documents
		Vector <Document> hits = new Vector();
		for(String titre : relevantDocs) {
			String searchQueryTitle = titre;
			QueryParser queryParserTitle = new QueryParser(Version.LUCENE_36, LuceneConstants.FILE_NAME, analyzer);
			Query queryTitle = queryParserTitle.parse(searchQueryTitle);
			topDocs = searcher.search(queryTitle, 1);
			ScoreDoc scoreDoc = topDocs.scoreDocs[0];
			Document doc = searcher.doc(scoreDoc.doc);
			
			//Check the title of the doc added to the hits vector
			String docTitle = doc.getField(LuceneConstants.FILE_NAME).stringValue();
			System.out.println(docTitle);
			hits.add(doc);
		}
		
		/**
		 * Debugging
		 */
		/*QueryParser queryParser = new QueryParser(Version.LUCENE_36, LuceneConstants.CONTENTS, analyzer);
		Query initialQuery =  queryParser.parse(searchQuery);
		System.out.println("Topic id: " + topicId);
		System.out.println(initialQuery.toString());
		bufferedWriter.write("Topic id: " + topicId + "\n");
		bufferedWriter.write("Original query: " + initialQuery.toString() + "\n");*/
		
		//Extension of the request
		Query queryExtend = queryExpander.obtainExpandedQuery(searchQuery, hits , prop);
		
		//Write down the expanded query obtainded
		System.out.println(queryExtend.toString());
		bufferedWriter.write(queryExtend.toString() + "\n");
		
		//Close the bufferedWriter and searcher
		bufferedWriter.close();
		searcher.close();
	}

}
