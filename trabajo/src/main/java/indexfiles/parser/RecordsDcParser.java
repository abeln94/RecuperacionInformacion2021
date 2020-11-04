package indexfiles.parser;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;

/**
 * Parses a file in the recordsdc format
 */
public class RecordsDcParser extends BasicParser {

    /**
     * Adds the file content to the document according to the recordsdc format
     */
    protected void parseFileContent(FileInputStream fis, Document doc) throws Exception {

        // parse XML dom
        org.w3c.dom.Document xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fis);

        // add TextField (long fields)
        for (String tag : new String[]{"creator", "contributor", "description", "publisher", "subject", "title", "relation", "rights", "identifier"}) {
            NodeList list = xmlDoc.getElementsByTagName("dc:" + tag);
            for (int i = 0; i < list.getLength(); i++) {
                doc.add(new TextField(tag, list.item(i).getTextContent(), Field.Store.YES));
            }
        }

        // convert type
        {
            NodeList list = xmlDoc.getElementsByTagName("dc:type");
            for (int i = 0; i < list.getLength(); i++) {
                String text = list.item(i).getTextContent()
                        .replace("TFG", " Trabajo Fin de Grado ")
                        .replace("TFM", " Trabajo Fin de Master ")
                        .replace("PFC", " Proyecto Fin de Carrera ");
                doc.add(new TextField("type", text, Field.Store.YES));
            }
        }

        // convert language
        {
            NodeList list = xmlDoc.getElementsByTagName("dc:language");
            for (int i = 0; i < list.getLength(); i++) {
                String text = list.item(i).getTextContent()
                        .replace("en", " inglés ")
                        .replace("eng", " ingles ")
                        .replace("fre", " francés ")
                        .replace("ger", " alemán ")
                        .replace("ita", " italiano ")
                        .replace("por", " portugués ")
                        .replace("spa", " español ");
                doc.add(new TextField("language", text, Field.Store.YES));
            }
        }

        // add date elements
        NodeList list = xmlDoc.getElementsByTagName("dc:date");
        for (int i = 0; i < list.getLength(); i++) {
            // removes all non-digit elements
            doc.add(new TextField("date", list.item(i).getTextContent().replaceAll("[^\\d]", ""), Field.Store.YES));
        }

    }
}
