import searchfiles.extractor.Extractor;
import org.apache.lucene.search.Query;
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
                .addRequired("-index", "The filename of the index folder", v -> index = v)
                .addRequired("-infoNeeds", "The filename of the infoNeeds file, or '-' for standar input", v -> infoNeeds = v)
                .addRequired("-output", "The filename of the output file, or '-' for standar output", v -> output = v)
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

    }
}
