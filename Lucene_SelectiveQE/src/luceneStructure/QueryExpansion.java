package luceneStructure;

/**
 * @author Neil O. Rouben
 */

import java.io.*;
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

//Class in charge of query expansion processing
public class QueryExpansion {

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
	private org.apache.lucene.search.Searcher searcher; //Using Lucene's Searcher class and not the one of the package
	private Similarity similarity;
	private Vector<TermQuery> expandedTerms;
	private QueryParser queryParser;
	private static Logger logger = Logger.getLogger( "queryExpansion.log" );

	//--------------------------------------------------
	//Builder
	//--------------------------------------------------
	public QueryExpansion( Analyzer analyzer, org.apache.lucene.search.Searcher searcher, Similarity similarity, Properties prop ) {
		this.analyzer = analyzer;
		this.searcher = searcher;
		this.similarity = similarity;
		this.prop = prop;
		this.queryParser = new QueryParser(Version.LUCENE_36, LuceneConstants.CONTENTS, analyzer);
	}

	//--------------------------------------------------
	//Method fixing the number of documents whose terms will be analyzed for the extension and returning the extended request
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
	//Method returning the documents whose terms will be analyzed for the extension of the request .i.e. docs judged to be relevant
	//--------------------------------------------------
	private Vector<Document> getDocs( String query, Vector<Document> hits, Properties prop ) throws IOException {
		Vector<Document> vHits = new Vector<Document>();        
		String docSource = prop.getProperty( QueryExpansion.DOC_SOURCE_FLD );
		//Extract only as many docs as necessary
		int docNum = Integer.valueOf( prop.getProperty( QueryExpansion.DOC_NUM_FLD ) ).intValue();

		//Obtain docs from local hits
		if ( docSource.equals( QueryExpansion.DOC_SOURCE_LOCAL  ) ) {        
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
	//Method returning the extended query
	//--------------------------------------------------
	public Query expandQuery( String queryStr, Vector<Document> hits, Properties prop ) throws IOException, ParseException {
		//Load Necessary Values from Properties
		float alpha = Float.valueOf( prop.getProperty( QueryExpansion.ROCCHIO_ALPHA_FLD ) ).floatValue();
		float beta = Float.valueOf( prop.getProperty( QueryExpansion.ROCCHIO_BETA_FLD ) ).floatValue();
		float decay = Float.valueOf( prop.getProperty( QueryExpansion.DECAY_FLD, "0.0" ) ).floatValue(); //DECAY_FLD si définie sinon 0
		int docNum = Integer.valueOf( prop.getProperty( QueryExpansion.DOC_NUM_FLD ) ).intValue();
		int termNum = Integer.valueOf( prop.getProperty( QueryExpansion.TERM_NUM_FLD ) ).intValue();                         

		//Create combine documents term vectors - sum ( rel term vectors )
		Vector<QueryTermVector> docsTermVector = getDocsTerms( hits, docNum, analyzer );
		
		/**
		 * Debugging code
		 */
		//System.out.println("Nombre de  vecteurs de termes de requête -QueryTermVectors- dans docsTermVector : " + docsTermVector.size());
		
		//Adjust term features of the docs with alpha * query; and beta; and assign weights/boost to terms (tf*idf)
		Query expandedQuery = adjust( docsTermVector, queryStr, alpha, beta, decay, docNum, termNum );

		return expandedQuery;
	}

	//--------------------------------------------------
	//Method that adjusts the weight of the terms of the documents with Rocchio's formula: alpha query + beta, gives or adjusts the weight of the terms
	//--------------------------------------------------
	public Query adjust( Vector<QueryTermVector> docsTermsVector, String queryStr, 
			float alpha, float beta, float decay, int docsRelevantCount, 
			int maxExpandedQueryTerms )
					throws IOException, ParseException
	{
		Query expandedQuery;

		//setBoost of docs terms /!\
		Vector<TermQuery> docsTerms = setBoost( docsTermsVector, beta, decay );
		logger.finer( docsTerms.toString() );

		//setBoost of query terms /!\
		//Get queryTerms from the query
		QueryTermVector queryTermsVector = new QueryTermVector( queryStr, analyzer );
		int queryTermsNumber = queryTermsVector.size(); //To know the number of terms in the initial query
		
		/**
		 * Debugging
		 */		
		//System.out.println("Number of terms in the initial query " + queryTermsNumber);
		
		Vector<TermQuery> queryTerms = setBoost( queryTermsVector, alpha );

		//Combine weights according to expansion formula
		Vector<TermQuery> expandedQueryTerms = combine( queryTerms, docsTerms );
		setExpandedTerms( expandedQueryTerms ); 
		//Sort by boost=weight
		Comparator comparator = new QueryBoostComparator();
		Collections.sort( expandedQueryTerms, comparator );

		//Create Expanded Query
		/**
		 * Debugging code
		 */
		//System.out.println("Taille du vecteur de termes : " + expandedQueryTerms.size() + "\t" + "Nombre max de termes pour l'expension : " + maxExpandedQueryTerms );
		
		expandedQuery = mergeQueries( expandedQueryTerms, maxExpandedQueryTerms, queryTermsNumber );
		logger.finer( expandedQuery.toString() );

		return expandedQuery;
	}

	//--------------------------------------------------
	//Method that extracts terms from the relevant documents and adds them to the document term vector
	//--------------------------------------------------
	public Vector<QueryTermVector> getDocsTerms( Vector<Document> hits, int docsRelevantCount, Analyzer analyzer )
			throws IOException
	{     
		Vector<QueryTermVector> docsTerms = new Vector<QueryTermVector>();

		//Process each of the documents
		for ( int i = 0; ( (i < docsRelevantCount) && (i < hits.size()) ); i++ )
		{
			Document doc = hits.elementAt( i );
			//Get text of the document and append it
			StringBuffer docTxtBuffer = new StringBuffer();
			String[] docTxtFlds = doc.getValues( LuceneConstants.FILE_TEXT ); // Contents pour l'instant égal à NULL donc rien 
			/**
			 * Print de debugging
			 */
			//System.out.println("Test : "+ doc.get(LuceneConstants.FILE_TEXT));
			
			for ( int j = 0; j < docTxtFlds.length; j++ )
			{
				docTxtBuffer.append( docTxtFlds[j] + " " );
			}      

			//Create termVector and add it to vector
			QueryTermVector docTerms = new QueryTermVector( docTxtBuffer.toString(), analyzer );
			docsTerms.add(docTerms );
			
		}        

		return docsTerms;
	}

	//--------------------------------------------------
	//Method that merges queries so that the relevant terms added appear in the extended query
	//--------------------------------------------------
	public Query mergeQueries( Vector<TermQuery> termQueries, int maxTerms, int nbInitQueryTerms )
			throws ParseException
	{
		Query query;

		//Limits the number of document terms to retrieve: if the query has more than maxTerms, only maxTerms of terms will be retrieved
		int termCount = Math.min( termQueries.size(), maxTerms + nbInitQueryTerms );
		
		/**
		 * Debugging
		 */
		//System.out.println("Total number of terms that can compose the expanded query " + termQueries.size());
		
		//Create Query String
		StringBuffer qBuf = new StringBuffer();
		for ( int i = 0; i < termCount; i++ )
		{
			TermQuery termQuery = termQueries.elementAt(i); 
			Term term = termQuery.getTerm();
			/**
			 * Debugging code
			 */
			//System.out.println(term.toString());
			
			qBuf.append( term.text() + "^" + termQuery.getBoost() + " " );
			logger.finest( term + " : " + termQuery.getBoost() );
		}
		
		/**
		 * Debugging code
		 */
		//System.out.println("Valeur du nombre de termes à ajouter à la requête : " +termCount + ", plus petite valeur entre : " +termQueries.size() + " et " + maxTerms);
		
		// Parse StringQuery to create Query
		logger.fine( qBuf.toString() );
		/**
		 * Debugging code
		 */
		//System.out.println(qBuf.toString());
		
		query = queryParser.parse( qBuf.toString());//Modification pour incompatibilité de version
		/**
		 * Debugging
		 */
		//System.out.println(query.toString());
		
		logger.fine( query.toString() );

		return query;
	}

	//--------------------------------------------------
	//Method that adjusts the weight of the terms in the initial query, weight = (tf*idf)
	//--------------------------------------------------
	public Vector<TermQuery> setBoost( QueryTermVector termVector, float factor )
			throws IOException
	{
		Vector<QueryTermVector> v = new Vector<QueryTermVector>();
		v.add( termVector );

		return setBoost( v, factor, 0 );
	}

	//--------------------------------------------------
	//Method that assigns a weight to the query terms and to the relevant documents most interesting terms, weight = (tf*idf)
	//--------------------------------------------------
	public Vector<TermQuery> setBoost( Vector<QueryTermVector> docsTerms, float factor, float decayFactor )
			throws IOException
	{
		Vector<TermQuery> terms = new Vector<TermQuery>();

		//setBoost for each of the terms of each of the docs
		for ( int g = 0; g < docsTerms.size(); g++ )
		{
			QueryTermVector docTerms = docsTerms.elementAt( g );
			String[] termsTxt = docTerms.getTerms();
			int[] termFrequencies = docTerms.getTermFrequencies();

			//Increase decay
			float decay = decayFactor * g;

			// Populate terms: with TermQuries and set boost
			for ( int i = 0; i < docTerms.size(); i++ )
			{
				//Create Term
				String termTxt = termsTxt[i];
				Term term = new Term( LuceneConstants.CONTENTS, termTxt );

				//Calculate weight
				float tf = termFrequencies[i];
				float idf = similarity.idfExplain(term, searcher).getIdf();//Modification pour incompatibilité de version Lucene
				float weight = tf * idf;
				
				/**
				 * Debugging code
				 */
				//System.out.println("term :" + term.text() + " tf: " + tf + " idf: " + idf + " weight: " + weight);
				
				// Adjust weight by decay factor
				weight = weight - (weight * decay);
				logger.finest("weight: " + weight);

				//Create TermQuery and add it to the collection
				TermQuery termQuery = new TermQuery( term );
				//Calculate and set boost
				termQuery.setBoost( factor * weight );
				terms.add( termQuery );
			}
		}

		//Get rid of duplicates by merging termQueries with equal terms
		merge( terms );		

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
					//Add boost factors of terms
					term.setBoost( term.getBoost() + tmpTerm.getBoost() );
					//Delete unnecessary term
					terms.remove( j );					
					//Decrement j so that term is not skipped
					j--;
				}
			}
		}
	}


	//--------------------------------------------------
	//Method that combines the weights in according to the expansion formula
	//--------------------------------------------------
	public Vector<TermQuery> combine( Vector<TermQuery> queryTerms, Vector<TermQuery> docsTerms )
	{
		Vector<TermQuery> terms = new Vector<TermQuery>();
		//Add Terms from the docsTerms
		terms.addAll( docsTerms );
		//Add Terms from queryTerms: if term already exists just increment its boost
		for ( int i = 0; i < queryTerms.size(); i++ )
		{
			TermQuery qTerm = queryTerms.elementAt(i);
			TermQuery term = find( qTerm, terms );
			//Term already exists update its boost
			if ( term != null )
			{
				float weight = qTerm.getBoost() + term.getBoost();
				term.setBoost( weight );
			}
			//Term does not exist; add it
			else
			{
				terms.add( qTerm );
			}
		}

		return terms;
	}

	//--------------------------------------------------
	// Method that find the terms that are equal
	//--------------------------------------------------
	public TermQuery find( TermQuery term, Vector<TermQuery> terms )
	{
		TermQuery termF = null;

		Iterator<TermQuery> iterator = terms.iterator();
		while ( iterator.hasNext() )
		{
			TermQuery currentTerm = iterator.next();
			if ( term.getTerm().equals( currentTerm.getTerm() ) )
			{
				termF = currentTerm;
				logger.finest( "Term Found: " + term );
			}
		}

		return termF;
	}

	//--------------------------------------------------
	// Method that returns the extension terms of the query
	//--------------------------------------------------
	public Vector<TermQuery> getExpandedTerms()
	{
		int termNum = Integer.valueOf( prop.getProperty( QueryExpansion.TERM_NUM_FLD ) ).intValue();
		Vector<TermQuery> terms = new Vector<TermQuery>();

		// Return only necessary number of terms
		List<TermQuery> list = this.expandedTerms.subList( 0, termNum );
		terms.addAll( list );

		return terms;
	}

	//--------------------------------------------------
	// Method that initializes the extension terms of the query
	//--------------------------------------------------
	private void setExpandedTerms( Vector<TermQuery> expandedTerms )
	{
		this.expandedTerms = expandedTerms;
	}

	//--------------------------------------------------
	//Méthod Main containing the tests of the class 
	//--------------------------------------------------
	public static void main(String[] args) throws IOException, ParseException {

		//Initializing a query expander
		Properties prop = new Properties();
		StopWords stopWords = new StopWords();
		stopWords.init();
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_36, stopWords.getStopSet());
		String indexDirectoryPath = LuceneConstants.INDEX_DIR;
		Directory indexDirectory = FSDirectory.open(new File(indexDirectoryPath));
		org.apache.lucene.search.Searcher searcher = new IndexSearcher(indexDirectory);
		Similarity similarity = new DefaultSimilarity();

		QueryExpansion queryExp = new QueryExpansion(analyzer, searcher, similarity, prop);
		
		//Defining a query results array
		TopDocs topDocs;

		//Formulating a query
		String searchQuery = "coronavirus origin what is the origin of COVID-19"; //User Query
		QueryParser queryParser = new QueryParser(Version.LUCENE_36,LuceneConstants.CONTENTS, analyzer);
		Query query = queryParser.parse(searchQuery); //Transformation of the user query into a weighted system queryExp
		System.out.println("Original query : " + query.toString());
		
		//Document vector generation
		Vector <Document> hits = new Vector();
		String [] relevantsDocs = {"3b5jzndg.txt","3b5jzndg.txt", "jmrg4oeb.txt", "jmrg4oeb.txt", "87d7gzgb.txt"};
		for (String titre : relevantsDocs) {
			String searchQueryTitle = titre;
			QueryParser queryParserTitle = new QueryParser(Version.LUCENE_36, LuceneConstants.FILE_NAME, analyzer);
			Query queryTitle = queryParserTitle.parse(searchQueryTitle);
			topDocs = searcher.search(queryTitle, 1);
			ScoreDoc scoreDoc = topDocs.scoreDocs[0];
			Document doc = searcher.doc(scoreDoc.doc);
			String docTitle = doc.getField(LuceneConstants.FILE_NAME).stringValue();
			System.out.println(docTitle);
			hits.add(doc);
		}
		
		//Getting the properties of our QueryExpansion object from the data in the file queryExpansion.properties
		FileReader propertiesReader = new FileReader("queryExpansion.properties");
		prop.load(propertiesReader);
		
		//Extension of the request
		Query queryExtend = queryExp.obtainExpandQuery(searchQuery, hits , prop);
		System.out.println("Extended query : " + queryExtend.toString());
		
		//Starting the search with the extended query
		topDocs = searcher.search(queryExtend, LuceneConstants.MAX_SEARCH);
		
		//Return of the request
		System.out.println("Rank after the query expend");
		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			Document doc = searcher.doc(scoreDoc.doc);
			System.out.println(doc.get(LuceneConstants.FILE_NAME) +" :\t" + scoreDoc.score);
		}
	}
}
