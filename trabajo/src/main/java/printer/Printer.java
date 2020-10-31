package printer;

import extractor.ConsoleExtractor;
import extractor.FileNeedsExtractor;
import org.apache.lucene.document.Document;

import java.io.IOException;
import java.util.List;

/**
 * Prints a search result
 */
public abstract class Printer {

    /**
     * Factory builder.
     * If file is null or '-' returns a {@link ConsolePrinter}
     * Otherwise returns a {@link FileNeedsPrinter}
     */
    static public Printer build(String file) throws IOException {
        if(file == null || file.equals("-"))
            return new ConsolePrinter();
        else
            return new FileNeedsPrinter(file);
    }

    /**
     * Prints the search result
     */
    abstract public void print(String id, List<Document> docs) throws IOException;
}
