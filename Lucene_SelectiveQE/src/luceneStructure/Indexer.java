package luceneStructure;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

//Class for indexing documents in the collection
public class Indexer {

	private IndexWriter writer; //The IndexWriter allows to create or update indexes during the indexing phase

	//--------------------------------------------------
	//Builder: initializes the indexWriter
	//--------------------------------------------------
	public Indexer(String indexDirectoryPath) throws IOException {
		//This directory will contain the indexes, set in the repository ~/TREC-COVID19/Covid_IR_System/Index 
		Directory indexDirectory = FSDirectory.open(new File(indexDirectoryPath));
		
		//Initialize the writer analyzer
		StopWords stopWords = new StopWords();
		stopWords.init();
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_36, stopWords.getStopSet());

		//Create the indexer: initialization of the indexWriter to with a standard analyzer.
		//The analyzer analyses the documents looking for the tokens that we want to index
		writer = new IndexWriter(indexDirectory, 
				analyzer,true, 
				IndexWriter.MaxFieldLength.UNLIMITED);
	}

	//--------------------------------------------------
	//Method returning the text of a file as a string
	//--------------------------------------------------
	public static String getText(File file) throws IOException {	
		StringBuilder stringBuilder = new StringBuilder();
		String ls = System.getProperty("line.separator"); //(\n)
		String line;
		try {
			Scanner myReader = new Scanner(file); //File reader
			while (myReader.hasNextLine()) {
				line = myReader.nextLine();
				stringBuilder.append(line);
				stringBuilder.append(ls);
			}
			myReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return stringBuilder.toString();
	}

	//--------------------------------------------------
	//Method closing the indewWriter
	//--------------------------------------------------
	public void close() throws CorruptIndexException, IOException {
		writer.close();
	}

	//--------------------------------------------------
	//Method indexing a file: allows to obtain an index-structured document from a file
	//--------------------------------------------------
	private Document getDocument(File file) throws IOException {
		Document document = new Document();//Objet Lucene obtenu en structurant le contenu d'un fichier (File Java) par le découpage en Index

		//Building the different fields composing the indexed Lucene documents

		//Indexing terms contained in the file content
		Field contentField = new Field(LuceneConstants.CONTENTS, new FileReader(file));
		//Index file name
		Field fileNameField = new Field(LuceneConstants.FILE_NAME,
				file.getName(),Field.Store.YES,Field.Index.NOT_ANALYZED);
		//Index file path
		Field filePathField = new Field(LuceneConstants.FILE_PATH,
				file.getCanonicalPath(),Field.Store.YES,Field.Index.NOT_ANALYZED);
		//index file text (unanalysed raw content)
		Field fileTextField = new Field(LuceneConstants.FILE_TEXT,
				getText(file),Field.Store.YES,Field.Index.NOT_ANALYZED);

		document.add(contentField);
		document.add(fileNameField);
		document.add(filePathField);
		document.add(fileTextField);

		return document;
	}   

	//--------------------------------------------------
	//Method adding a document to the Lucene Indexed documents collection
	//--------------------------------------------------
	private void indexFile(File file) throws IOException {
		System.out.println("Indexing "+file.getCanonicalPath());
		Document document = getDocument(file);
		writer.addDocument(document);
	}

	//--------------------------------------------------
	//Method constituting the list of files to be indexed
	//--------------------------------------------------
	public int createIndex(String dataDirPath, TextFileFilter filter) throws IOException {
		//get all files in the data directory, c'est à dire le dossier ~/TREC-COVID19/LuceneTest/Data
		File[] files = new File(dataDirPath).listFiles();
		for (File file : files) {
			if(!file.isDirectory()
					&& !file.isHidden()
					&& file.exists()
					&& file.canRead()
					&& filter.accept(file)
					){
				indexFile(file);
			}
		}
		return writer.numDocs();

	}
}
