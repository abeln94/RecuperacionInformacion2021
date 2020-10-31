package extractor;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * Extracts text to search
 */
public abstract class Extractor {

    /**
     * Factory builder.
     * If file is null or '-' returns a {@link ConsoleExtractor}
     * Otherwise returns a {@link FileNeedsExtractor}
     */
    static public Extractor build(String file) throws ParserConfigurationException, SAXException, IOException {
        if (file == null || file.equals("-"))
            return new ConsoleExtractor();
        else
            return new FileNeedsExtractor(file);
    }

    /**
     * @return true if there is another text to search
     */
    abstract public boolean hasNext();

    /**
     * @return the next text to search
     */
    abstract public Element getNext();

    // ------------------------- data -------------------------

    /**
     * A search element
     */
    static public class Element {

        /**
         * The id of the search
         */
        public String id;

        /**
         * The search text
         */
        public String text;

        public Element(String id, String text) {
            this.id = id;
            this.text = text;
        }
    }

}
