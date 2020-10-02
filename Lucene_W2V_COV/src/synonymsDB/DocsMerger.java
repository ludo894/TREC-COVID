package synonymsDB;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import luceneStructure.TextFileFilter;

//Class that gives a method to merge documents with a .txt extension
public class DocsMerger {

	//--------------------------------------------------
	//Method that merge all .txt files present in a specified repository
	//--------------------------------------------------
	public void merge (String dirPath) throws IOException {
		FileWriter fileWriter = new FileWriter("mergedData.txt",true);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		Scanner reader = null;
		String line;
		String ls = System.getProperty("line.separator"); //(\n)
		//Get all the documents that must be merge
		luceneStructure.TextFileFilter filter = new TextFileFilter();
		File[] files = new File(dirPath).listFiles();
		for (File file : files) {
			if(!file.isDirectory()
					&& !file.isHidden()
					&& file.exists()
					&& file.canRead()
					&& filter.accept(file)
					){
				//Performe the merge
				reader = new Scanner(file);
				while (reader.hasNextLine()) {
					line = reader.nextLine();
					bufferedWriter.append(line);
					bufferedWriter.append(ls);
				}
			}
		}
		reader.close();
		bufferedWriter.close();
		fileWriter.close();
	}

	//--------------------------------------------------
	//Method Main for testing
	//--------------------------------------------------
	public static void main(String[] args) {
		DocsMerger merger = new DocsMerger();
		//String dirPath = "/home/ludo/TREC-COVID19/Word2Vec/TrainingText/Data";
		String dirPath = "/home/ludo/TREC-COVID19/Covid_IR_System/Data";
		try {
			merger.merge(dirPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
