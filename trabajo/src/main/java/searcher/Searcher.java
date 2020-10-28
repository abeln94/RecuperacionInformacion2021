package searcher;/*
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

import indexer.Indexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.ext.Extensions.Pair;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple command-line based search demo.
 */
public class Searcher {

    // ------------------------- app -------------------------

    private final QueryParser parser;
    private final IndexSearcher searcher;

    public Searcher(String index) throws Exception {
        // init parser
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
        Analyzer analyzer = new SpanishAnalyzer();
        parser = new QueryParser("content", analyzer);

        // init searcher
        searcher = new IndexSearcher(reader);
//        searcher.setSimilarity(new ClassicSimilarity()); // from the teacher
    }

    // ------------------------- search -------------------------

    /**
     * Performs a search from an input text
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

        return search(parseQuery(line));
    }


    // ------------------------- query -------------------------

    // special search queries
    private static final List<Pair<String, Function<Matcher, Query>>> specials = new ArrayList<>();

    static {
        // spatial
        specials.add(new Pair<>("spatial:([^ ]*)", match -> {
            double[] west_east_south_north = Arrays.stream(match.group(1).split(",")).mapToDouble(Double::parseDouble).toArray();
            return new BooleanQuery.Builder()
                    .add(DoublePoint.newRangeQuery(Indexer.WEST, Double.NEGATIVE_INFINITY, west_east_south_north[1]), BooleanClause.Occur.MUST)
                    .add(DoublePoint.newRangeQuery(Indexer.EAST, west_east_south_north[0], Double.POSITIVE_INFINITY), BooleanClause.Occur.MUST)
                    .add(DoublePoint.newRangeQuery(Indexer.SOUTH, Double.NEGATIVE_INFINITY, west_east_south_north[3]), BooleanClause.Occur.MUST)
                    .add(DoublePoint.newRangeQuery(Indexer.NORTH, west_east_south_north[2], Double.POSITIVE_INFINITY), BooleanClause.Occur.MUST)
                    .build();
        }));

        // temporal
        specials.add(new Pair<>("temporal:\\[\\s*([^\\s]*)\\s*TO\\s*([^\\s]*)\\s*\\]", match -> {
            String startDate = match.group(1).replaceAll("[^\\d]", "");
            while (startDate.length() < 8) startDate += "0";
            String endDate = match.group(2).replaceAll("[^\\d]", "");
            while (endDate.length() < 8) endDate += "9";
            return new BooleanQuery.Builder()
                    // end >= startDate -> end € [startDate, oo)
                    .add(DoublePoint.newRangeQuery(Indexer.END, Double.parseDouble(startDate), Double.POSITIVE_INFINITY), BooleanClause.Occur.MUST)
                    // begin <= endDate -> begin € [-oo, endDate]
                    .add(DoublePoint.newRangeQuery(Indexer.BEGIN, Double.NEGATIVE_INFINITY, Double.parseDouble(endDate)), BooleanClause.Occur.MUST)
                    .build();
        }
        ));
    }

    /**
     * Converts
     *
     * @param line this search text
     * @return to a query
     */
    public Query parseQuery(String line) throws ParseException {
        // prepare query
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        // get and remove the special parts (if found)
        for (Pair<String, Function<Matcher, Query>> special : specials) {
            Pattern match = Pattern.compile(special.cur);
            Matcher matcher = match.matcher(line);
            while (matcher.find()) {
                // add to builder
                builder.add(special.cud.apply(matcher), BooleanClause.Occur.SHOULD);
            }
            line = matcher.replaceAll("");
        }

        // the rest is a text query
        if (!line.isEmpty())
            builder.add(parser.parse(line), BooleanClause.Occur.SHOULD);

        return builder.build();
    }

    /**
     * Performs a search
     *
     * @param query from this query
     * @return and returns a list of the names
     */
    private String[] search(Query query) throws Exception {
        return Arrays.stream(searcher.search(query, Integer.MAX_VALUE).scoreDocs).map(doc -> {
            try {
                String path = searcher.doc(doc.doc).get("path");
                return path.substring(path.lastIndexOf("\\") + 1);
            } catch (IOException e) {
                e.printStackTrace();
                return "?";
            }
        }).toArray(String[]::new);
    }
}