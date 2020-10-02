package luceneStructure;


import java.io.File;
import java.io.FileFilter;

//Text file filter class implementing the file filter interface
public class TextFileFilter implements FileFilter {
	
	//--------------------------------------------------
	//Méthod verifying the document is a text type file
	//--------------------------------------------------
	@Override
	public boolean accept(File pathname) {
		return pathname.getName().toLowerCase().endsWith(".txt");
	}

}
