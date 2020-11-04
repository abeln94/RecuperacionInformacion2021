package searchfiles.queryfy;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import tools.CustomAnalyzer;

import java.util.regex.Pattern;

/**
 * Parses a text and returns the corresponding query
 */
public class Querify {

    private final Analyzer spanishAnalyzer = new CustomAnalyzer(true);
    private final Analyzer simpleAnalyzer = new CustomAnalyzer(false);

    /**
     * Parses a text and returns the corresponding query
     */
    public Query parse(String line) throws ParseException {

        // prepare query
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        // add all terms
        for (Element field : fields) {
            builder.add(
                    new BoostQuery(
                            new QueryParser(field.field, field.analyze ? spanishAnalyzer : simpleAnalyzer).parse(line),
                            field.pattern.matcher(line).find() ? field.matchBoost : field.normalBoost
                    ),
                    BooleanClause.Occur.SHOULD
            );
        }

        BooleanQuery query = builder.build();
        System.out.println("Query: " + query);
        return query;
    }

    // search data
    // values totally arbitrary, TODO needs tweaking
    private static final Element[] fields = new Element[]{
            new Element("contributor", 1.5f, 2f, "profesor|dirigido", false),
            new Element("creator", 1.5f, 2f, "alumno|autor|realizado", false),
            new Element("date", 1.3f, 1.6f, "publicación|publicado", false),
            new Element("description", 1f, 1.5f, "descripción", true),
            new Element("language", 1.3f, 1.7f, "idioma", false),
            new Element("publisher", 1.4f, 1.7f, "universidad|departamento|área", false),
            new Element("subject", 1f, 1.5f, "tema", true),
            new Element("title", 1f, 1.5f, "título", true),
            new Element("type", 1.3f, 1.6f, "trabajo|tesis|proyecto", false),
    };

    // ------------------------- data -------------------------

    /**
     * A field querifyer
     */
    private static class Element {
        /**
         * The field to search
         */
        private final String field;
        /**
         * Boost value if the pattern is not found
         */
        private final float normalBoost;
        /**
         * Boost value if the pattern is found
         */
        private final float matchBoost;
        /**
         * Patter to search to change the boost value
         */
        private final Pattern pattern;
        /**
         * If true, the text will be analyzer
         * If true, will only be tokenized
         */
        private final boolean analyze;

        public Element(String field, float normalBoost, float matchBoost, String regexp, boolean analyze) {
            this.field = field;
            this.normalBoost = normalBoost;
            this.matchBoost = matchBoost;
            this.pattern = Pattern.compile(regexp);
            this.analyze = analyze;
        }
    }

}
