package searchfiles.printer;

import org.apache.lucene.index.IndexableField;
import searchfiles.searcher.Searcher.Element;

import java.util.List;

/**
 * Prints the search result into console
 */
public class ConsolePrinter extends Printer {

    // how many elements to show
    public static final int TOP = 5;

    @Override
    public void print(String id, List<Element> results) {
        // get total returned elements
        int n = results.size();
        System.out.println(n + " results for id=" + id);

        // cap
        if (n > TOP) {
            System.out.println("(Showing only top " + TOP + ")");
            results = results.subList(0, TOP);
        }

        // print
        for (int i = 0; i < results.size(); i++) {
            Element element = results.get(i);
            System.out.println("***** result " + (i + 1) + " *****");
            System.out.println("Score = " + element.explanation.get());
            System.out.println("Doc fields:");
            for (IndexableField field : element.document.getFields()) {
                System.out.println("- " + field.name() + " > " + field.stringValue());
            }
            System.out.println();
        }
        System.out.println();
    }
}
