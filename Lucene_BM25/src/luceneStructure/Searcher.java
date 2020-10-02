package luceneStructure;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class Searcher {
	
	IndexReader indexReader;
	IndexSearcher indexSearcher;
	QueryParser queryParser;
	Query query;

	//--------------------------------------------------
	//Builder : taking the absolute path of the Index directory in parameter
	//--------------------------------------------------
	public Searcher(String indexDir) throws IOException {		
		
		//Initialize the writer analyzer
		StopWords stopWords = new StopWords();
		stopWords.init();
		Analyzer analyzer = new StandardAnalyzer(stopWords.getStopSet());
		indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDir)));
		indexSearcher = new IndexSearcher(indexReader);
		Similarity similarity = new BM25Similarity(); //Use ClassicSimilarity for a TFIDF based search
		indexSearcher.setSimilarity(similarity); //By default Lucene 8 uses BM25Similarity so if similarity is initialized on BM25, this line is useless
		queryParser = new QueryParser(LuceneConstants.CONTENTS,analyzer);
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

	public static void main(String[] args) throws IOException, ParseException {
		
		//initialize a searcher
		Searcher searcher = new Searcher(LuceneConstants.INDEX_DIR);
		TopDocs topDocs = searcher.search("coronavirus origin what is the origin of COVID-19");
		for (ScoreDoc sd : topDocs.scoreDocs) {
			Document doc = searcher.getDocument(sd);
			System.out.println(doc.getField(LuceneConstants.FILE_NAME).getCharSequenceValue());
		}

	}

}
