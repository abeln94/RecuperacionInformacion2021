import searcher.QueryManager;
import searcher.Searcher;

public class SearchFiles {

    private static String index = "index";
    private static String infoNeeds = null;
    private static String output;
    private static String query = null;
    private static int repeat = 0;
    private static boolean debug = false;


    /**
     * Simple command-line based search demo.
     */
    public static void main(String[] args) throws Exception {
        String usage = "Usage:\tjava SearchFiles [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]";

        if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
            System.out.println(usage);
            System.exit(0);
        }

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
            } else if ("-query".equals(args[i])) {
                query = args[i + 1];
                i++;
            } else if ("-repeat".equals(args[i])) {
                repeat = Integer.parseInt(args[i + 1]);
                i++;
            } else if ("-d".equals(args[i])) {
                debug = true;
            }
        }

        QueryManager qm = new QueryManager(new Searcher(index), output);

        if (query != null) {
            // input string
            if (repeat > 0) {
                qm.benchmark(query, repeat);
                return;
            }

            // run that string
            qm.search(query);
            return;
        }

        if(infoNeeds != null){
            qm.fromFile(infoNeeds);
            return;
        }

        // run interactive
        qm.interactive();

    }
}
