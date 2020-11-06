package indexfiles.parser;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.w3c.dom.NodeList;
import tools.CustomAnalyzer;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.util.HashMap;

/**
 * Parses a file in the recordsdc format
 */
public class RecordsDcParser extends BasicParser {

    public static final String FIELD_CREATOR = "creator";
    public static final String FIELD_CONTRIBUTOR = "contributor";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_PUBLISHER = "publisher";
    public static final String FIELD_SUBJECT = "subject";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_RELATION = "relation";
    public static final String FIELD_RIGHTS = "rights";
    public static final String FIELD_IDENTIFIER = "identifier";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_LANGUAGE = "language";
    public static final String FIELD_DATE = "date";

    public static final String PREFIX_DC = "dc:";

    /**
     * Adds the file content to the document according to the recordsdc format
     */
    protected void parseFileContent(FileInputStream fis, Document doc) throws Exception {

        // parse XML dom
        org.w3c.dom.Document xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fis);

        // add TextField (long fields)
        for (String tag : new String[]{FIELD_CREATOR, FIELD_CONTRIBUTOR, FIELD_DESCRIPTION, FIELD_PUBLISHER, FIELD_SUBJECT, FIELD_TITLE, FIELD_RELATION, FIELD_RIGHTS, FIELD_IDENTIFIER}) {
            NodeList list = xmlDoc.getElementsByTagName(PREFIX_DC + tag);
            for (int i = 0; i < list.getLength(); i++) {
                doc.add(new TextField(tag, list.item(i).getTextContent(), Field.Store.YES));
            }
        }

        // convert type
        {
            NodeList list = xmlDoc.getElementsByTagName(PREFIX_DC + FIELD_TYPE);
            for (int i = 0; i < list.getLength(); i++) {
                String text = list.item(i).getTextContent()
                        .replace("TFG", " Trabajo Fin de Grado ")
                        .replace("TFM", " Trabajo Fin de Master ")
                        .replace("PFC", " Proyecto Fin de Carrera ");
                doc.add(new TextField(FIELD_TYPE, text, Field.Store.YES));
            }
        }

        // convert language
        {
            NodeList list = xmlDoc.getElementsByTagName(PREFIX_DC + FIELD_LANGUAGE);
            for (int i = 0; i < list.getLength(); i++) {
                String text = list.item(i).getTextContent()
                        .replace("en", " inglés ")
                        .replace("eng", " ingles ")
                        .replace("fre", " francés ")
                        .replace("ger", " alemán ")
                        .replace("ita", " italiano ")
                        .replace("por", " portugués ")
                        .replace("spa", " español ");
                doc.add(new TextField(FIELD_LANGUAGE, text, Field.Store.YES));
            }
        }

        // add date elements
        NodeList list = xmlDoc.getElementsByTagName(PREFIX_DC + FIELD_DATE);
        for (int i = 0; i < list.getLength(); i++) {
            // removes all non-digit elements
            doc.add(new TextField(FIELD_DATE, list.item(i).getTextContent().replaceAll("[^\\d]", ""), Field.Store.YES));
        }
    }

    // ------------------------- analyzers -------------------------

    @Override
    public Analyzer getAnalyzer() {
        HashMap<String, Analyzer> fieldAnalyzers = new HashMap<>();
        CustomAnalyzer c = new CustomAnalyzer(true);
        fieldAnalyzers.put(FIELD_DESCRIPTION, c);
        fieldAnalyzers.put(FIELD_SUBJECT, c);
        fieldAnalyzers.put(FIELD_TITLE, c);
        return new PerFieldAnalyzerWrapper(new CustomAnalyzer(false), fieldAnalyzers);
    }
}
