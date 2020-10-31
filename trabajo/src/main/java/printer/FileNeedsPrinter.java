package printer;

import org.apache.lucene.document.Document;

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
    public void print(String id, List<Document> docs) throws IOException {
        // each document as one line
        for (Document doc : docs) {
            // {id} {name of the file}
            String path = doc.get("path");
            path = path.substring(path.lastIndexOf("\\") + 1);
            output.write(id + "\t" + path + "\n");
        }
    }
}
