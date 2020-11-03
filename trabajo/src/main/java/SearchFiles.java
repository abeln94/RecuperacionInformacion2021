import org.apache.lucene.search.Query;
import searchfiles.extractor.Extractor;
import searchfiles.printer.Printer;
import searchfiles.queryfy.Querify;
import searchfiles.searcher.Searcher;
import tools.ArgsParser;

import java.io.IOException;
import java.util.List;

/**
 * The application to search the files
 */
public class SearchFiles {

    private static String index = "index";
    private static String infoNeeds = null;
    private static String output = null;


    /**
     * Simple command-line based search demo.
     */
    public static void main(String[] args) throws Exception {

        new ArgsParser("This searches the documents in DOCS_PATH from an existing Lucene index in INDEX_PATH that can be created with IndexFiles")
                .addRequired("-index", "The filename of the index folder", 1, v -> index = v.get(0))
                .addRequired("-infoNeeds", "The filename of the infoNeeds file, or '-' for standar input", 1, v -> infoNeeds = v.get(0))
                .addRequired("-output", "The filename of the output file, or '-' for standar output", 1, v -> output = v.get(0))
                .parse(args);

        // init
        Extractor extractor = Extractor.build(infoNeeds);
        Querify querify = new Querify();
        Searcher searcher = new Searcher(index);
        Printer printer = Printer.build(output);

        // app
        while (extractor.hasNext()) {
            try {
                Extractor.Element element = extractor.getNext();
                Query query = querify.parse(element.text);
                List<Searcher.Element> docs = searcher.search(query);
                printer.print(element.id, docs);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        printer.close();
    }
}
