package luceneStructure;

import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.io.BufferedWriter;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

//Class managing interaction with the user
public class LuceneTester {
	private String indexDir = LuceneConstants.INDEX_DIR; //Index directory
	private String dataDir = LuceneConstants.DATA_DIR; //Documents directory
	private Indexer indexer; //Declaration of the file indexer
	private TopicsFileReader topicsFileReader;

	public static void main(String[] args) {
		LuceneTester tester = new LuceneTester();
		tester.menu();
	}

	//--------------------------------------------------
	//Method for creating the index
	//--------------------------------------------------
	private void createIndex() throws IOException {
		indexer = new Indexer(indexDir);
		int numIndexed;
		long startTime = System.currentTimeMillis();
		numIndexed = indexer.createIndex(dataDir, new TextFileFilter());
		long endTime = System.currentTimeMillis();
		indexer.close();
		System.out.println(numIndexed + " File indexed, time taken: "
				+ (endTime-startTime) +" ms");		
	}

	//--------------------------------------------------
	//Method displaying a command line menu
	//--------------------------------------------------
	private void menu () {
		Scanner sc = new Scanner(System.in);
		char userAnswer;
		boolean queryExpansion = false;
		String topicsFileName;
		try {

			//Document indexing request to the user
			System.out.println("Index documents needed? [o/n]");
			userAnswer = sc.nextLine().charAt(0);
			if (userAnswer == 'o') {
				this.createIndex();
			}

			//Ask the user if he wants to launch a query
			System.out.println("Performe a run? [o/n]");
			userAnswer = sc.nextLine().charAt(0);
			if (userAnswer == 'o') {

				//Query on the base of indexed documents
				System.out.println("Absolute Path of the topics file?");//File containing the list of queries must be placed in the Java project directory
				topicsFileName = sc.nextLine(); // /home/ludo/TREC-COVID19/Covid_IR_System/Topics/topics_no_nar
				
				//Ask the user if he wants to use the query expansion feature
				System.out.println("Use the query expansion feature? [o/n] ");
				userAnswer = sc.nextLine().charAt(0);
				
				if (userAnswer == 'o') {
					queryExpansion = true;
				}
				
				//Initializing the query file reader
				topicsFileReader = new TopicsFileReader(queryExpansion);
				
				//Analyses the queries and generating the output file
				topicsFileReader.readFile(topicsFileName);

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		sc.close();
	}

}
