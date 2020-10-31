package printer;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import java.util.List;

/**
 * Prints the search result into console
 */
public class ConsolePrinter extends Printer {
    @Override
    public void print(String id, List<Document> docs) {
        // get total returned elements
        int n = docs.size();
        System.out.println(n + " results for id=" + id);

        // cap
        if (n > 10) {
            System.out.println("(Showing only top 10)");
            n = 10;
        }

        // print
        for (Document doc : docs.subList(0, n)) {
            for (IndexableField field : doc.getFields()) {
                System.out.println(field.name() + " > "+field.stringValue());
            }
            System.out.println();
        }
        System.out.println();
    }
}
