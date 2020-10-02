package luceneStructure;

/**
 * @author Mourot Ludovic
 */

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

//Class adding to the original query the synonyms obtained with the word embedding process
public class WordEmbedding {

	//Constants with values sets by the file : /home/ludo/TREC-COVID19/Covid_IR_System/LuceneQE/Lucene_Word2Vec/queryExpansion.properties
	public static final String DOC_SOURCE_FLD = "QE.doc.source"; //Documents directory
	public static final String DOC_SOURCE_LOCAL = "local"; //Use to indicate that we use a local source

	public static final String ROCCHIO_ALPHA_FLD = "rocchio.alpha";
	public static final String ROCCHIO_BETA_FLD = "rocchio.beta";
	public static final String DECAY_FLD = "QE.decay"; //Considering that the pseudo feedback contains only relevant feedback, DECAY = 0

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
	public WordEmbedding( Analyzer analyzer, org.apache.lucene.search.Searcher searcher, Similarity similarity, Properties prop ) {
		this.analyzer = analyzer;
		this.searcher = searcher;
		this.similarity = similarity;
		this.prop = prop;
		this.queryParser = new QueryParser(Version.LUCENE_36, LuceneConstants.CONTENTS, analyzer);
	}

	//--------------------------------------------------
	//Method returning the extended query
	//--------------------------------------------------
	public Query wordEmbeddingQuery( String queryStr, Properties prop, Boolean fixedSynonymsNb ) throws IOException, ParseException {
		//Load Necessary Values from Properties
		float alpha = Float.valueOf( prop.getProperty( WordEmbedding.ROCCHIO_ALPHA_FLD ) ).floatValue();
		float beta = Float.valueOf( prop.getProperty( WordEmbedding.ROCCHIO_BETA_FLD ) ).floatValue();
		float decay = Float.valueOf( prop.getProperty( WordEmbedding.DECAY_FLD, "0.0" ) ).floatValue(); //DECAY_FLD id defined otherwise 0
		//Create a vector that contained the vector of synonyms
		Vector<QueryTermVector> synonymsVector = null;
		if (fixedSynonymsNb) {
			synonymsVector = getTermsSynonyms( queryStr, analyzer );
		} else {
			synonymsVector = getWeightedTermsSynonyms( queryStr, analyzer, searcher, similarity );
		}
		

		/**
		 * Debugging code
		 */
		//System.out.println("Nombre de  vecteurs de termes de requête -QueryTermVectors- dans docsTermVector : " + docsTermVector.size());

		//Adjust query terms and synonyms weight with alpha and beta Rocchio's parameters
		Query expandedQuery = adjust( synonymsVector, queryStr, alpha, beta, decay);

		return expandedQuery;
	}

	//--------------------------------------------------
	//Method returning the vector of synonyms wrap in a vector : one synonym will be added for each term of the original query
	//--------------------------------------------------
	public Vector <QueryTermVector> getTermsSynonyms (String queryStr, Analyzer analyzer){
		//Declare the vector containing the vector of synonyms
		Vector <QueryTermVector> synonyms = new Vector <QueryTermVector>();

		//Parse the search query and retrieve its terms
		QueryTermVector queryTermsVector = new QueryTermVector( queryStr, analyzer );
		String [] termsText = queryTermsVector.getTerms();

		//Search for each term synonyms and append them
		StringBuffer synBuffer = new StringBuffer();
		for (int i = 0; i < queryTermsVector.size(); i++) {
			String termText = termsText[i];
			String synonym = getSynonyms(termText, 1);
			synBuffer.append(synonym + " ");
		}
		//create synonym vector and add it to synonyms
		QueryTermVector synonymsVec = new QueryTermVector (synBuffer.toString(), analyzer);
		synonyms.add(synonymsVec);

		return synonyms;
	}
	
	//--------------------------------------------------
	//Alternative method returning the vector of synonyms wrap in a vector : one or two synonyms will be added to the query depending on the weight of the terms
	//--------------------------------------------------
	public Vector <QueryTermVector> getWeightedTermsSynonyms (String queryStr, Analyzer analyzer, org.apache.lucene.search.Searcher searcher, Similarity similarity)
			throws IOException {
		//Declare the vector containing the vector of synonyms
		Vector <QueryTermVector> synonyms = new Vector <QueryTermVector>();

		//Parse the search query and retrieve its terms
		QueryTermVector queryTermsVector = new QueryTermVector( queryStr, analyzer );
		String [] termsText = queryTermsVector.getTerms();
		int [] termsFreq = queryTermsVector.getTermFrequencies();

		//Create a list to store the weight of each initial query term
		List<Float> weightsList = new ArrayList<Float> ();

		//Calculate weight and add it to the weight list
		for (int i = 0; i < queryTermsVector.size(); i++) {
			String termText = termsText[i];
			Term term = new Term (LuceneConstants.CONTENTS, termText);
			float tf = termsFreq[i];
			float idf = similarity.idfExplain(term, searcher).getIdf();
			float weight = tf * idf;
			weightsList.add(weight);
		}

		//Calculate the average weight of the terms
		float totalWeight = 0.0f;
		for (float f : weightsList) {
			totalWeight = totalWeight + f;
		}
		float averageWeight = totalWeight/weightsList.size();

		//Search for each term synonyms and append them
		StringBuffer synBuffer = new StringBuffer();
		String synonym;
		for (int i = 0; i < queryTermsVector.size(); i++) {
			//Create the termText
			String termText = termsText[i];

			//Get its weight
			float weight = weightsList.get(i);

			//Find two term synonyms if its weight is over the average
			if (weight >= averageWeight) {
				synonym = getSynonyms(termText, 1);
				synBuffer.append(synonym + " ");
				synonym = getSynonyms(termText, 2);
				synBuffer.append(synonym + " ");
			} else { //Fin only one synonym if the term weight is below the average
				synonym = getSynonyms(termText, 1);
				synBuffer.append(synonym + " ");
			}
		}
		//create synonym vector and add it to synonyms
		QueryTermVector synonymsVec = new QueryTermVector (synBuffer.toString(), analyzer);
		synonyms.add(synonymsVec);

		return synonyms;
	}

	//--------------------------------------------------
	//Method returning the synonym(s) of a query term specified in parameter
	//--------------------------------------------------
	public String getSynonyms(String word, int synRank) {
		String synonym = "";

		//Connection to the database
		try {
			Connection co = DriverManager.getConnection("jdbc:mysql://localhost:3306/synonymsdb?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC","ludo", "treccovid19");
			System.out.println("Successfully connected to the database ");
			Statement st = co.createStatement();

			//Get the synonym from the database
			String query = "SELECT synonym1, synonym2 FROM covidsynonyms WHERE word = " + "'" + word + "'";
			ResultSet rlt = st.executeQuery(query);
			if (rlt.next()) {
				synonym = rlt.getString(synRank);
			} else {
				System.out.println("No synonym in base for the word " + word);
			}
			rlt.close();
			st.close();
			co.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return synonym;
	}

	//--------------------------------------------------
	//Method that adjusts the weight of the query terms and their synonyms
	//--------------------------------------------------
	public Query adjust( Vector<QueryTermVector> synonymsVector, String queryStr, 
			float alpha, float beta, float decay )
					throws IOException, ParseException
	{
		Query wordEmbeddedQuery;

		//setBoost of docs terms /!\
		Vector<TermQuery> synonymsTerms = setBoost( synonymsVector, beta, decay );
		logger.finer( synonymsTerms.toString() );

		//setBoost of query terms /!\
		//Get queryTerms from the query
		QueryTermVector queryTermsVector = new QueryTermVector( queryStr, analyzer );

		/**
		 * Debugging
		 */		
		//System.out.println("Number of terms in the initial query " + queryTermsNumber);

		Vector<TermQuery> queryTerms = setBoost( queryTermsVector, alpha );

		//Combine weights according to expansion formula
		Vector<TermQuery> wordEmbeddedTerms = combine( queryTerms, synonymsTerms );
		setExpandedTerms( wordEmbeddedTerms ); 
		//Sort by boost=weight
		Comparator comparator = new QueryBoostComparator();
		Collections.sort( wordEmbeddedTerms, comparator );

		//Create Expanded Query
		/**
		 * Debugging code
		 */
		//System.out.println("Taille du vecteur de termes : " + expandedQueryTerms.size() + "\t" + "Nombre max de termes pour l'expension : " + maxExpandedQueryTerms );
		int totalTermsNumber = wordEmbeddedTerms.size(); //To know the number of terms in the initial query
		wordEmbeddedQuery = mergeQueries( wordEmbeddedTerms, totalTermsNumber );
		logger.finer( wordEmbeddedQuery.toString() );

		return wordEmbeddedQuery;
	}

	//--------------------------------------------------
	//Method that create a query from the words and synonyms contained in the wordEmbeddedTerms vector
	//--------------------------------------------------
	public Query mergeQueries( Vector<TermQuery> termQueries, int totalTermsNumber )
			throws ParseException
	{
		Query query;

		/**
		 * Debugging
		 */
		//System.out.println("Total number of terms that can compose the expanded query " + termQueries.size());

		//Create Query String
		StringBuffer qBuf = new StringBuffer();
		for ( int i = 0; i < totalTermsNumber; i++ )
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
	//Method that assigns a weight to the original query terms and to the synonyms, weight = (tf*idf)
	//--------------------------------------------------
	public Vector<TermQuery> setBoost( Vector<QueryTermVector> termVector, float factor, float decayFactor )
			throws IOException
	{
		Vector<TermQuery> terms = new Vector<TermQuery>();

		//setBoost for each of the terms of each of the docs
		for ( int g = 0; g < termVector.size(); g++ )
		{
			QueryTermVector docTerms = termVector.elementAt( g );
			String[] termsTxt = docTerms.getTerms();
			int[] termFrequencies = docTerms.getTermFrequencies();

			/**
			 * Debugging
			 */
			/*System.out.println("factor : " + factor);
			for (int i = 0; i < docTerms.size(); i++) {
				System.out.print(termsTxt[i]+ " : ");
				System.out.println(termFrequencies[i]);
			}*/

			//Increase decay
			float decay = decayFactor * g;

			// Populate terms: with TermQueries and set boost
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
	public Vector<TermQuery> combine( Vector<TermQuery> queryTerms, Vector<TermQuery> synonymsTerms )
	{
		Vector<TermQuery> terms = new Vector<TermQuery>();
		//Add Terms from the docsTerms
		terms.addAll( synonymsTerms );
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

		WordEmbedding queryExp = new WordEmbedding(analyzer, searcher, similarity, prop);

		//Formulating a query
		String searchQuery = "coronavirus origin what is the origin of COVID-19"; //User Query
		QueryParser queryParser = new QueryParser(Version.LUCENE_36,LuceneConstants.CONTENTS, analyzer);
		Query query = queryParser.parse(searchQuery); //Transformation of the user query into a weighted system queryExp
		System.out.println(query.toString());
		
		/**
		 * Debugging
		 */
		Set stopWordsSet = StandardAnalyzer.STOP_WORDS_SET;
		Iterator iterator = stopWordsSet.iterator();
		while (iterator.hasNext()) {
			System.out.println((char[])iterator.next());
		}
		
		//Document vector generation
		Vector <Document> hits = new Vector();
		TopDocs topDocs;
		topDocs = searcher.search(query, LuceneConstants.MAX_SEARCH);
		
		/**
		 * Debugging
		 */
		System.out.println("Rank before the query expend");

		System.out.println("Documents sorties en résultat de la requête initiale :");
		for(ScoreDoc scoreDoc : topDocs.scoreDocs) {
			Document doc = searcher.doc(scoreDoc.doc);
			hits.add(doc);
			System.out.println(doc.get(LuceneConstants.FILE_NAME) + " : " + scoreDoc.score);
		}

		//Getting the properties of our QueryExpansion object from the data in the file queryExpansion.properties
		FileReader propertiesReader = new FileReader("queryExpansion.properties");
		prop.load(propertiesReader);

		//Extension of the request
		Query queryExtend = queryExp.wordEmbeddingQuery(searchQuery, prop, LuceneConstants.FIXED_SYNONYMS_NB);
		System.out.println(queryExtend.toString());

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
