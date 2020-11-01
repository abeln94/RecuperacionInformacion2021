package queryfy;

import indexer.Indexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.ext.Extensions;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a text and returns the corresponding query
 */
public class Querify {

    private final QueryParser parser;

    public Querify() {
        Analyzer analyzer = new SpanishAnalyzer();
        parser = new MultiFieldQueryParser(new String[]{"description", "subject", "title"}, analyzer);
    }

    /**
     * Parses a text and returns the corresponding query
     */
    public Query parse(String line) throws ParseException {

        // prepare query
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        // get and remove the special parts (if found)
        for (Extensions.Pair<String, Function<Matcher, Query>> special : specials) {
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

        BooleanQuery query = builder.build();
        System.out.println("Query: "+query);
        return query;
    }

    // special search queries
    private static final List<Extensions.Pair<String, Function<Matcher, Query>>> specials = new ArrayList<>();

    static {
        // spatial
        specials.add(new Extensions.Pair<>("spatial:([^ ]*)", match -> {
            double[] west_east_south_north = Arrays.stream(match.group(1).split(",")).mapToDouble(Double::parseDouble).toArray();
            return new BooleanQuery.Builder()
                    .add(DoublePoint.newRangeQuery(Indexer.WEST, Double.NEGATIVE_INFINITY, west_east_south_north[1]), BooleanClause.Occur.MUST)
                    .add(DoublePoint.newRangeQuery(Indexer.EAST, west_east_south_north[0], Double.POSITIVE_INFINITY), BooleanClause.Occur.MUST)
                    .add(DoublePoint.newRangeQuery(Indexer.SOUTH, Double.NEGATIVE_INFINITY, west_east_south_north[3]), BooleanClause.Occur.MUST)
                    .add(DoublePoint.newRangeQuery(Indexer.NORTH, west_east_south_north[2], Double.POSITIVE_INFINITY), BooleanClause.Occur.MUST)
                    .build();
        }));

        // temporal
        specials.add(new Extensions.Pair<>("temporal:\\[\\s*([^\\s]*)\\s*TO\\s*([^\\s]*)\\s*\\]", match -> {
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

}
