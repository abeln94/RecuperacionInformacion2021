package extractor;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Extracts texts from a fileneeds file
 */
public class FileNeedsExtractor extends Extractor {

    private final LinkedList<Element> needs = new LinkedList<>();

    public FileNeedsExtractor(String file) throws IOException, ParserConfigurationException, SAXException {
        // open file
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new FileInputStream(file));

        // parse needs
        NodeList nodes = document.getElementsByTagName("informationNeed");
        for (int i = 0; i < nodes.getLength(); i++) {
            // each informationNeed
            org.w3c.dom.Element need = ((org.w3c.dom.Element) nodes.item(i));

            // extract text
            String id = need.getElementsByTagName("identifier").item(0).getTextContent();
            String query = need.getElementsByTagName("text").item(0).getTextContent();
            needs.add(new Element(id, query));
        }
    }

    @Override
    public boolean hasNext() {
        return !needs.isEmpty();
    }

    @Override
    public Element getNext() {
        return needs.removeFirst();
    }
}
