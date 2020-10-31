package printer;

import org.apache.lucene.document.Document;
import searcher.Searcher.Element;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Prints the search result into a file (in fileNeeds format)
 */
public class FileNeedsPrinter extends Printer {
    private FileWriter output;

    public FileNeedsPrinter(String file) throws IOException {
        this.output = new FileWriter(file);
    }

    @Override
    public void print(String id, List<Element> results) throws IOException {
        // each document as one line
        for (Element element : results) {
            // {id} {name of the file}
            String path = element.document.get("path");
            path = path.substring(path.lastIndexOf("\\") + 1);
            output.write(id + "\t" + path + "\n");
        }
    }
}
