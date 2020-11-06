package indexfiles.parser;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Parses a File and converts it to a Document in a basic mode
 */
public class BasicParser {

    public static final String FIELD_PATH = "path";
    public static final String FIELD_MODIFIED = "modified";
    public static final String FIELD_CONTENT = "content";

    /**
     * Parses the file and returns a document
     */
    public Document parseFileContent(File file) {

        // index file content
        try (FileInputStream fis = new FileInputStream(file)) {

            // make a new, empty document
            Document doc = new Document();

            // parse File properties
            parseFileProperties(file, doc);

            // parse File content
            parseFileContent(fis, doc);

            // return
            return doc;

        } catch (Exception e) {
            // error reading
            System.err.println("Can't parse file " + file);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Adds the file properties of the file to the document
     */
    protected void parseFileProperties(File file, Document doc) {
        // Add the path of the file as a field named "path".  Use a
        // field that is indexed (i.e. searchable), but don't tokenize
        // the field into separate words and don't index term frequency
        // or positional information:
        Field pathField = new StringField(FIELD_PATH, file.getPath(), Field.Store.YES);
        doc.add(pathField);

        // Add the last modified date of the file a field named "modified".
        // Use a StoredField to return later its value as a response to a query.
        // This indexes to milli-second resolution, which
        // is often too fine.  You could instead create a number based on
        // year/month/day/hour/minutes/seconds, down the resolution you require.
        // For example the long value 2011021714 would mean
        // February 17, 2011, 2-3 PM.
        DateFormat dtFormat = SimpleDateFormat.getDateTimeInstance();
        doc.add(new StoredField(FIELD_MODIFIED, dtFormat.format(file.lastModified())));
    }

    /**
     * Adds the file content to the document (basic mode, no parsing)
     */
    protected void parseFileContent(FileInputStream fis, Document doc) throws Exception {
        doc.add(new TextField(FIELD_CONTENT, new BufferedReader(new InputStreamReader(fis, "UTF-8"))));
    }

    /**
     * @return the analyzer that should be used for this document
     */
    public Analyzer getAnalyzer(){
        return new StandardAnalyzer();
    }
}
