import extractor.Extractor;
import org.apache.lucene.search.Query;
import printer.ConsolePrinter;
import printer.Printer;
import queryfy.Querify;
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
            }
        }

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
