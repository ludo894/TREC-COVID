package luceneStructure;

import java.sql.*; 

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

public class WordEmbeddingBeta {

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

	public String getSynonyms(String word, int synRank) {
		String synonym = "";

		//Connection to the database
		try {
			Connection co = DriverManager.getConnection("jdbc:mysql://localhost:3306/synonymsdb?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC","ludo", "treccovid19");
			System.out.println("Successfully connected to the database ");
			Statement st = co.createStatement();

			//Get the synonym from the database
			String query = "SELECT synonym1, synonym2 FROM synonyms WHERE word = " + "'" + word + "'";
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

	public static void main(String[] args ) throws IOException, ParseException {
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);
		String indexDirectoryPath = LuceneConstants.INDEX_DIR;
		Directory indexDirectory = FSDirectory.open(new File(indexDirectoryPath));
		org.apache.lucene.search.Searcher searcher = new IndexSearcher(indexDirectory);
		Similarity similarity = new DefaultSimilarity();

		//TEST getTermsSynonyms

		//we creation
		WordEmbeddingBeta we = new WordEmbeddingBeta();

		//Get the vector of synonyms vector for the specified search query
		Vector <QueryTermVector> synonyms = new Vector <QueryTermVector>();
		String queryStr = "rabbit changed ideas";
		synonyms = we.getTermsSynonyms(queryStr, analyzer);

		//Print the synonyms
		for (int i = 0; i < synonyms.size(); i++) {
			QueryTermVector synonymsVec = synonyms.elementAt(i);
			String [] synonymsTxt = synonymsVec.getTerms();
			for (int j = 0; j < synonymsVec.size(); j++) {
				System.out.println(synonymsTxt[j]);
			}
		}

		//TEST getWeightedTermsSynonyms

		//Get the vector of synonyms (one or two depending on the weight of the term)
		Vector <QueryTermVector> moreSynonyms = new Vector <QueryTermVector> ();
		String queryStr2 = "drug title virus stop animal death health";
		moreSynonyms = we.getWeightedTermsSynonyms(queryStr2, analyzer, searcher, similarity);

		//Print the synonyms
		for (int i = 0; i < moreSynonyms.size(); i++) {
			QueryTermVector synonymsVec = moreSynonyms.elementAt(i);
			String [] synonymsTxt = synonymsVec.getTerms();
			for (int j = 0; j < synonymsVec.size(); j++) {
				System.out.println(synonymsTxt[j]);
			}
		}

	}

}