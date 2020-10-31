import extractor.Extractor;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import printer.Printer;
import queryfy.Queryfy;
import searcher.Searcher;

import java.io.IOException;
import java.util.List;

/**
 * The application to search the files
 */
public class SearchFiles {

    private static String index = "index";
    private static String infoNeeds = null;
    private static String output = null;
    private static boolean debug = false;


    /**
     * Simple command-line based search demo.
     */
    public static void main(String[] args) throws Exception {

        // check args
        if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
            System.out.println("Usage:\tjava SearchFiles [-index dir] [-infoNeeds file] [-output file]");
            System.exit(0);
        }

        // parse args
        for (int i = 0; i < args.length; i++) {
            if ("-index".equals(args[i])) {
                index = args[i + 1];
                i++;
            } else if ("-infoNeeds".equals(args[i])) {
                infoNeeds = args[i + 1];
                i++;
            } else if ("-output".equals(args[i])) {
                output = args[i + 1];
                i++;
            } else if ("-d".equals(args[i])) {
                debug = true;
            }
        }

        // init
        Extractor extractor = Extractor.build(infoNeeds);
        Queryfy queryfy = new Queryfy();
        Searcher searcher = new Searcher(index);
        Printer printer = Printer.build(output);

        // app
        while (extractor.hasNext()) {
            try {
                Extractor.Element element = extractor.getNext();
                Query query = queryfy.parse(element.text);
                List<Document> docs = searcher.search(query);
                printer.print(element.id, docs);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
