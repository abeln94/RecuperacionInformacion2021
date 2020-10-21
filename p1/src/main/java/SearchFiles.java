/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple command-line based search demo.
 */
public class SearchFiles {

    private static String field = "contents";
    private static String index = "index";
    private static String queries = null;
    private static int repeat = 0;
    private static boolean raw = false;
    private static String queryString = null;
    private static int hitsPerPage = 10;


    /**
     * Simple command-line based search demo.
     */
    public static void main(String[] args) throws Exception {
        String usage =
                "Usage:\tjava org.apache.lucene.demo.SearchFiles [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n\nSee http://lucene.apache.org/core/4_1_0/demo/ for details.";
        if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
            System.out.println(usage);
            System.exit(0);
        }

        for (int i = 0; i < args.length; i++) {
            if ("-index".equals(args[i])) {
                index = args[i + 1];
                i++;
            } else if ("-field".equals(args[i])) {
                field = args[i + 1];
                i++;
            } else if ("-queries".equals(args[i])) {
                queries = args[i + 1];
                i++;
            } else if ("-query".equals(args[i])) {
                queryString = args[i + 1];
                i++;
            } else if ("-repeat".equals(args[i])) {
                repeat = Integer.parseInt(args[i + 1]);
                i++;
            } else if ("-raw".equals(args[i])) {
                raw = true;
            } else if ("-paging".equals(args[i])) {
                hitsPerPage = Integer.parseInt(args[i + 1]);
                if (hitsPerPage <= 0) {
                    System.err.println("There must be at least 1 hit per page.");
                    System.exit(1);
                }
                i++;
            }
        }


        SearchFiles app = new SearchFiles();

        if (queryString != null) {
            // input string
            if (repeat > 0) {
                // repeat & time as benchmark
                System.out.println("Time: " + app.benchmarkSearch(queryString) + "ms");
                return;
            }

            // run that string
            System.out.println(Arrays.toString(app.search(queryString)));
            return;
        }

        if (queries != null) {
            // run multiple queries simultaneously
            try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.print(line + ": ");
                    System.out.println(Arrays.toString(app.search(line)));
                }
            }
            return;
        }

        // run interactive
        app.interactiveSearch();

    }

    // ------------------------- app -------------------------

    private final QueryParser parser;
    private final IndexSearcher searcher;

    public SearchFiles() throws Exception {
        // init parser
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
        Analyzer analyzer = new SpanishAnalyzer2();
        parser = new QueryParser(field, analyzer);

        // init searcher
        searcher = new IndexSearcher(reader);
//        searcher.setSimilarity(new ClassicSimilarity()); // from the teacher
    }

    // ------------------------- search -------------------------

    /**
     * Performs a search from a input text
     *
     * @param line from this text
     * @return and returns the matching docs identifiers
     */
    public String[] search(String line) throws Exception {
        if (line == null) {
            return null;
        }

        line = line.trim();
        if (line.length() == 0) {
            return null;
        }

        return returnDetailsSearch(parseQuery(line));
    }

    /**
     * Asjks the user for a serach text, searches, and repeats
     */
    public void interactiveSearch() throws Exception {

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

                Query query = parseQuery(line);

                System.out.println("Searching for: " + query.toString(field));

                showPaginatedSearch(in, query);

            }
        }
    }

    /**
     * Benchmarks @repeat times
     *
     * @param line this search text
     * @return adn returns the total time
     */
    public long benchmarkSearch(String line) throws Exception {
        Query query = parseQuery(line);

        Date start = new Date();
        for (int i = 0; i < repeat; i++) {
            searcher.search(query, 100);
        }
        Date end = new Date();
        return end.getTime() - start.getTime();
    }

    // ------------------------- query -------------------------

    /**
     * Converts
     *
     * @param line this search text
     * @return to a query
     */
    public Query parseQuery(String line) throws ParseException {
        // prepare query
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        // get and remove the spatial part (if found)
        Pattern match = Pattern.compile("spatial:([^ ]*)");
        Matcher matcher = match.matcher(line);
        while (matcher.find()) {
            String part = matcher.group(1);
            // spatial query
            double[] west_east_south_north = Arrays.stream(part.split(",")).mapToDouble(Double::parseDouble).toArray();

            builder.add(new BooleanQuery.Builder()
                            .add(DoublePoint.newRangeQuery(IndexFiles.WEST, Double.NEGATIVE_INFINITY, west_east_south_north[1]), BooleanClause.Occur.MUST)
                            .add(DoublePoint.newRangeQuery(IndexFiles.EAST, west_east_south_north[0], Double.POSITIVE_INFINITY), BooleanClause.Occur.MUST)
                            .add(DoublePoint.newRangeQuery(IndexFiles.SOUTH, Double.NEGATIVE_INFINITY, west_east_south_north[3]), BooleanClause.Occur.MUST)
                            .add(DoublePoint.newRangeQuery(IndexFiles.NORTH, west_east_south_north[2], Double.POSITIVE_INFINITY), BooleanClause.Occur.MUST)
                            .build()
                    , BooleanClause.Occur.SHOULD);
        }
        line = matcher.replaceAll("");


        // the rest is a text query
        if (!line.isEmpty())
            builder.add(parser.parse(line), BooleanClause.Occur.SHOULD);

        return builder.build();
    }

    /**
     * Performs a search
     *
     * @param query from this query
     * @return and returns a list of the results as 2-digits code
     */
    public String[] returnDetailsSearch(Query query) throws Exception {
        return Arrays.stream(searcher.search(query, Integer.MAX_VALUE).scoreDocs).map(doc -> {
            try {
                String path = searcher.doc(doc.doc).get("path");
                return path.substring(path.lastIndexOf("\\") + 1).substring(0, 2);
            } catch (IOException e) {
                e.printStackTrace();
                return "??";
            }
        }).toArray(String[]::new);
    }

    /**
     * This demonstrates a typical paging search scenario, where the search engine presents
     * pages of size n to the user. The user can then go to the next page if interested in
     * the next hits.
     * <p>
     * When the query is executed for the first time, then only enough results are collected
     * to fill 5 result pages. If the user wants to page beyond this limit, then the query
     * is executed another time and all hits are collected.
     */
    public void showPaginatedSearch(BufferedReader in, Query query) throws IOException {

        // Collect enough docs to show 5 pages
        TopDocs results = searcher.search(query, 5 * hitsPerPage);
        ScoreDoc[] hits = results.scoreDocs;

        int numTotalHits = Math.toIntExact(results.totalHits.value);
        System.out.println(numTotalHits + " total matching documents");

        int start = 0;
        int end = Math.min(numTotalHits, hitsPerPage);

        while (true) {
            if (end > hits.length) {
                System.out.println("Only results 1 - " + hits.length + " of " + numTotalHits + " total matching documents collected.");
                System.out.println("Collect more (y/n) ?");
                String line = in.readLine();
                if (line.length() == 0 || line.charAt(0) == 'n') {
                    break;
                }

                hits = searcher.search(query, numTotalHits).scoreDocs;
            }

            end = Math.min(hits.length, start + hitsPerPage);

            for (int i = start; i < end; i++) {
                if (raw) {                              // output raw format
                    System.out.println("doc=" + hits[i].doc + " score=" + hits[i].score);
                    continue;
                }

                Document doc = searcher.doc(hits[i].doc);
                String path = doc.get("path");
                if (path != null) {
                    System.out.println((i + 1) + ". " + path);
                } else {
                    System.out.println((i + 1) + ". " + "No path for this document");
                }
                System.out.println("  modified: " + doc.get("modified"));

                // explain the scoring function
                System.out.println(searcher.explain(query, hits[i].doc));

            }

            if (end == 0) {
                break;
            }

            if (numTotalHits >= end) {
                boolean quit = false;
                while (true) {
                    System.out.print("Press ");
                    if (start - hitsPerPage >= 0) {
                        System.out.print("(p)revious page, ");
                    }
                    if (start + hitsPerPage < numTotalHits) {
                        System.out.print("(n)ext page, ");
                    }
                    System.out.println("(q)uit or enter number to jump to a page.");

                    String line = in.readLine();
                    if (line.length() == 0 || line.charAt(0) == 'q') {
                        quit = true;
                        break;
                    }
                    if (line.charAt(0) == 'p') {
                        start = Math.max(0, start - hitsPerPage);
                        break;
                    } else if (line.charAt(0) == 'n') {
                        if (start + hitsPerPage < numTotalHits) {
                            start += hitsPerPage;
                        }
                        break;
                    } else {
                        int page = Integer.parseInt(line);
                        if ((page - 1) * hitsPerPage < numTotalHits) {
                            start = (page - 1) * hitsPerPage;
                            break;
                        } else {
                            System.out.println("No such page");
                        }
                    }
                }
                if (quit) break;
                end = Math.min(numTotalHits, start + hitsPerPage);
            }
        }
    }
}