package searchfiles.printer;

import searchfiles.searcher.Searcher.Element;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Prints the search result into a file (in fileNeeds format)
 */
public class FileNeedsPrinter extends Printer {
    private final FileWriter output;

    public FileNeedsPrinter(String file) throws IOException {
        this.output = new FileWriter(file);
    }

    @Override
    public void print(String id, List<Element> results) throws IOException {
        // each document as one line
        System.out.println("Printing " + results.size() + " results to file");
        for (Element element : results) {
            // {id} {name of the file}
            String path = element.document.get("path");
            path = path.substring(path.lastIndexOf("\\") + 1);
            output.write(id + "\t" + path + "\n");
        }
    }

    public void close(){
        try {
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
