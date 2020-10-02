package luceneStructure;


import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

//Class to create queries on the indexed documents collection
public class Searcher {

	IndexSearcher indexSearcher;
	QueryParser queryParser;
	Query query;

	//--------------------------------------------------
	//Builder : taking the absolute path of the Index directory in parameter
	//--------------------------------------------------
	public Searcher(String indexDirectoryPath) throws IOException {
		Directory indexDirectory = FSDirectory.open(new File(indexDirectoryPath));
		
		//Initialize the writer analyzer
		StopWords stopWords = new StopWords();
		stopWords.init();
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_36, stopWords.getStopSet());
		indexSearcher = new IndexSearcher(indexDirectory);
		queryParser = new QueryParser(Version.LUCENE_36,
				LuceneConstants.CONTENTS,
				analyzer);
	}
	
	//--------------------------------------------------
	//Method that perform a query search in  the indexed documents collection, taking the user's query as a parameter
	//--------------------------------------------------
	public TopDocs search( String searchQuery) throws IOException, ParseException {
		query = queryParser.parse(searchQuery);
		return indexSearcher.search(query, LuceneConstants.MAX_SEARCH);
	}
	
	//--------------------------------------------------
	//Method for retrieving  a ranked document from the collection
	//--------------------------------------------------
	public Document getDocument(ScoreDoc scoreDoc) throws CorruptIndexException, IOException {
		return indexSearcher.doc(scoreDoc.doc);	
	}
	
	//--------------------------------------------------
	//Method for closing the searcher
	//--------------------------------------------------
	public void close() throws IOException {
		indexSearcher.close();
	}

}
