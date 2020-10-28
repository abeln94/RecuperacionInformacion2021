package searcher;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class QueryManager {

    private final Searcher searcher;
    private FileWriter output = null;

    public QueryManager(Searcher searcher, String output) {
        this.searcher = searcher;

        if (output != null) {
            try {
                this.output = new FileWriter(output);
            } catch (IOException e) {
                System.err.println("Can't open output file");
                e.printStackTrace();
            }
        }
    }

    /**
     * Asks the user for text, searches, and repeats
     */
    public void interactive() throws Exception {

        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            while (true) {
                // prompt the user
                System.out.println("Enter query: ");

                String line = in.readLine();

                if (line == null) {
                    break;
                }

                line = line.trim();
                if (line.length() == 0) {
                    break;
                }

                search(line);

            }
        }
    }

    /**
     * Extracts the search elements from the file
     */
    public void fromFile(String file) {
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new FileInputStream(file));

            NodeList needs = document.getElementsByTagName("informationNeed");
            for (int i = 0; i < needs.getLength(); i++) {
                // each informationNeed
                Element need = ((Element) needs.item(i));

                // run query
                String id = need.getElementsByTagName("identifier").item(0).getTextContent();
                String query = need.getElementsByTagName("text").item(0).getTextContent();
                search(id, query);
            }
        } catch (Exception e) {
            // error
            e.printStackTrace();
        }
    }

    /**
     * Runs the 'query' search 'repeat' times and prints the total time
     */
    public void benchmark(String query, int repeat) {
        System.out.println("Benchmark query...");
        try {
            Date start = new Date();
            for (int i = 0; i < repeat; i++) {
                searcher.search(query);
            }
            Date end = new Date();
            System.out.println("...done in " + (end.getTime() - start.getTime()) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Runs a query search
     */
    public void search(String query) {
        search(null, query);
    }

    /**
     * Runs a query search associated with an identifier
     */
    public void search(String id, String query) {
        if (output == null) {
            System.out.println((id != null ? "[" + id + "] " : "") + "Searching: \"" + query + "\"");
        }

        try {
            String[] results = searcher.search(query);
            if (results == null || results.length == 0) {
                // no results
                if (output == null)
                    System.out.println("> No results");
            } else {
                // at least one result
                for (String result : results) {
                    if (output != null) output.write(id + "\t" + result + "\n");
                    else System.out.println("> " + result);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
