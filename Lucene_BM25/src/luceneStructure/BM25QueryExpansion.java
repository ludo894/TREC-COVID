package luceneStructure;

/**
 * @author Neil O. Rouben
 */

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.apache.lucene.queryparser.classic.ParseException;

//Class in charge of query expansion processing
public class BM25QueryExpansion {

	//Constants with values sets by the file : /home/ludo/TREC-COVID19/Covid_IR_System/LuceneQE/Lucene.3.6_QE/queryExpansion.properties
	public static final String DOC_SOURCE_FLD = "QE.doc.source"; //Documents directory
	public static final String DOC_SOURCE_LOCAL = "local"; //Local directory where to retrieve documents
	public static final String DOC_NUM_FLD = "QE.doc.num";

	public static final String ROCCHIO_ALPHA_FLD = "rocchio.alpha";
	public static final String ROCCHIO_BETA_FLD = "rocchio.beta";
	public static final String DECAY_FLD = "QE.decay"; //Considering that the pseudo feedback contains only relevant feedback, DECAY = 0
	public static final String TERM_NUM_FLD = "QE.term.num";




	//Attributes
	private Properties prop; //Obtained by the reading of queryExpansion.properties file by a FileReader
	private Analyzer analyzer;
	private IndexSearcher searcher; //Using Lucene's Searcher class and not the one of the package
	private Similarity similarity;
	private Vector<TermQuery> expandedTerms;
	private QueryParser queryParser;
	private static Logger logger = Logger.getLogger( "queryExpansion.log" );
	private HashMap<Integer, String> id2num = new HashMap<Integer, String>();
	private HashMap<String, Integer> num2id = new HashMap<String, Integer>();

	//--------------------------------------------------
	//Builder
	//--------------------------------------------------
	public BM25QueryExpansion( Analyzer analyzer, IndexSearcher searcher, Similarity similarity, Properties prop ) {
		this.analyzer = analyzer;
		this.searcher = searcher;
		this.similarity = similarity;
		this.prop = prop;
		this.queryParser = new QueryParser(LuceneConstants.CONTENTS, analyzer);
	}

	//--------------------------------------------------
	//Method fixing the number of documents whose terms will be analyzed for the extension and returning the extended query
	//--------------------------------------------------
	public Query obtainExpandQuery( String queryStr, Vector<Document> hits, Properties prop )
			throws IOException, ParseException
	{
		//Get Docs to be used in query expansion
		Vector<Document> vHits = getDocs( queryStr, hits, prop );
		/**
		 * Debugging code
		 */
		/*System.out.println("Documents pertinents obtenus : " + vHits.size());
		for (Document doc : vHits) {
			System.out.println(doc.get(LuceneConstants.FILE_NAME));
		}*/

		return expandQuery( queryStr, vHits, prop );
	}

	//--------------------------------------------------
	//Method returning the extended query
	//--------------------------------------------------
	public Query expandQuery( String queryStr, Vector<Document> hits, Properties prop ) throws IOException, ParseException {
		//Load Necessary Values from Properties
		float alpha = Float.valueOf( prop.getProperty( BM25QueryExpansion.ROCCHIO_ALPHA_FLD ) ).floatValue();
		float beta = Float.valueOf( prop.getProperty( BM25QueryExpansion.ROCCHIO_BETA_FLD ) ).floatValue();
		float decay = Float.valueOf( prop.getProperty( BM25QueryExpansion.DECAY_FLD, "0.0" ) ).floatValue(); //DECAY_FLD si définie sinon 0
		int docNum = Integer.valueOf( prop.getProperty( BM25QueryExpansion.DOC_NUM_FLD ) ).intValue();
		int termNum = Integer.valueOf( prop.getProperty( BM25QueryExpansion.TERM_NUM_FLD ) ).intValue();                         

		//Create combine documents term vectors - sum ( rel term vectors )
		Vector <Terms> docsTermVector = getDocsTerms( hits, docNum, analyzer );

		/**
		 * Debugging code
		 */
		//System.out.println("Nombre de  vecteurs de termes de requête -QueryTermVectors- dans docsTermVector : " + docsTermVector.size());

		//Adjust term features of the docs with alpha * query; and beta; and assign weights/boost to terms (tf*idf)
		Query expandedQuery = null;

		return expandedQuery;
	}

	//--------------------------------------------------
	//Method returning the documents whose terms will be analyzed for the extension of the request .i.e. docs judged to be relevant
	//--------------------------------------------------
	private Vector<Document> getDocs( String query, Vector<Document> hits, Properties prop ) throws IOException {
		Vector<Document> vHits = new Vector<Document>();        
		String docSource = prop.getProperty( BM25QueryExpansion.DOC_SOURCE_FLD );

		//Extract only as many docs as necessary
		int docNum = Integer.valueOf( prop.getProperty( BM25QueryExpansion.DOC_NUM_FLD ) ).intValue();

		//Obtain docs from local hits
		if ( docSource.equals( BM25QueryExpansion.DOC_SOURCE_LOCAL  ) ) {        
			//Records in a vector the documents considered as relevant according to the pseudo feedbacks method (Top n) 
			for ( int i = 0; ( ( i < docNum ) && ( i < hits.size() ) ); i++ )
			{
				vHits.add( hits.get(i) );
			}
		} else {
			throw new RuntimeException( docSource + ": is not implemented" );
		}            
		return vHits;
	}

	//--------------------------------------------------
	//Method that extracts terms from the relevant documents and adds them to the document term vector
	//--------------------------------------------------
	public Vector<Terms> getDocsTerms( Vector<Document> hits, int docsRelevantCount, Analyzer analyzer )
			throws IOException
	{     
		Vector<Terms> docsTerms = new Vector<Terms>();

		//Process each of the documents
		for ( int i = 0; ( (i < docsRelevantCount) && (i < hits.size()) ); i++ )
		{
			Document doc = hits.elementAt( i );
			int docid = num2id.get(doc.get(LuceneConstants.FILE_NUM));   

			//Create termVector and add it to vector
			IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(LuceneConstants.INDEX_DIR)));
			Terms docTerms = indexReader.getTermVector(docid, LuceneConstants.CONTENTS);
			docsTerms.add(docTerms );

		}        

		return docsTerms;
	}

	//--------------------------------------------------
	//Method that merges in one TermQuery vectors all the documents terms
	//--------------------------------------------------
	public Vector<TermQuery> mergeDocTerms( Vector<Terms> docsTerms, float factor, float decayFactor )
			throws IOException
	{
		Vector<TermQuery> terms = new Vector<TermQuery>();

		//Extracts terms of all documents terms vector
		for ( int g = 0; g < docsTerms.size(); g++ )
		{
			Terms docTerms = docsTerms.elementAt( g );
			TermsEnum termsEnum = docTerms.iterator();
			BytesRef brTerm;
			while ((brTerm = termsEnum.next()) != null )
			{	
				Term term = new Term( LuceneConstants.CONTENTS, brTerm.utf8ToString() );
				//Create TermQuery and add it to the collection
				TermQuery termQuery = new TermQuery( term );
				terms.add( termQuery );
			}
		}
		merge(terms);
		return terms;
	}

	//--------------------------------------------------
	//Method that delete the duplicates terms : merges query terms that have equal terms
	//--------------------------------------------------
	private void merge(Vector<TermQuery> terms) 
	{
		for ( int i = 0; i < terms.size(); i++ )
		{
			TermQuery term = terms.elementAt( i );
			//Itterate through terms and if term is equal then merge: add the boost; and delete the term
			for ( int j = i + 1; j < terms.size(); j++ )
			{
				TermQuery tmpTerm = terms.elementAt( j );

				//If equal then merge
				if ( tmpTerm.getTerm().text().equals( term.getTerm().text() ) )
				{
					//Delete unnecessary term
					terms.remove( j );					
					//Decrement j so that term is not skipped
					j--;
				}
			}
		}
	}

	//--------------------------------------------------
	//Matchs docNum with Lucene docID
	//--------------------------------------------------
	public void  mapDocid() {
		try {
			IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(LuceneConstants.INDEX_DIR)));
			int n = indexReader.maxDoc();

			for (int i = 0; i < n; i++) {
				Document doc = indexReader.document(i);

				// the doc.get pulls out the values stored - ONLY if you store the fields
				String docno = doc.get(LuceneConstants.FILE_NUM);
				id2num.put(i, docno);
				num2id.put(docno, i);
			}
		} catch (Exception e){
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());
		}
	}


}