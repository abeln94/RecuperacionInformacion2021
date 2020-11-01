package indexer;

import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Indexer {

    // properties
    public static final String WEST = "west";
    public static final String EAST = "east";
    public static final String SOUTH = "south";
    public static final String NORTH = "north";
    public static final String BEGIN = "begin";
    public static final String END = "end";

    // parameters
    private final String indexPath;
    private final boolean create;
    private final File docDir;
    private final boolean debug;

    public Indexer(String indexPath, String docsPath, boolean create, boolean debug) {

        this.indexPath = indexPath;

        docDir = new File(docsPath);
        if (!docDir.exists() || !docDir.canRead()) {
            System.out.println("Document directory '" + docDir.getAbsolutePath() + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        this.create = create;
        this.debug = debug;

    }


    public void run() throws IOException {
        Date start = new Date();
        System.out.println("Indexing to directory '" + indexPath + "'...");

        IndexWriterConfig iwc = new IndexWriterConfig(new SpanishAnalyzer());
        if (create) {
            // Create a new index in the directory, removing any
            // previously indexed documents:
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        } else {
            // Add new documents to an existing index:
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        }

        // Optional: for better indexing performance, if you
        // are indexing many documents, increase the RAM
        // buffer.  But if you do this, increase the max heap
        // size to the JVM (eg add -Xmx512m or -Xmx1g):
        //
        // iwc.setRAMBufferSizeMB(256.0);

        IndexWriter writer = new IndexWriter(FSDirectory.open(Paths.get(indexPath)), iwc);
        indexDocs(writer, docDir);

        // NOTE: if you want to maximize search performance,
        // you can optionally call forceMerge here.  This can be
        // a terribly costly operation, so generally it's only
        // worth it when your index is relatively static (ie
        // you're done adding documents to it):
        //
        // writer.forceMerge(1);

        writer.close();

        Date end = new Date();
        System.out.println(end.getTime() - start.getTime() + " total milliseconds");

    }

    /**
     * Indexes the given file using the given writer, or if a directory is given,
     * recurses over files and directories found under the given directory.
     * <p>
     * NOTE: This method indexes one document per input file.  This is slow.  For good
     * throughput, put multiple documents into your input file(s).  An example of this is
     * in the benchmark module, which can create "line doc" files, one document per line,
     * using the
     * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
     * >WriteLineDocTask</a>.
     *
     * @param writer Writer to the index where the given file/dir info will be stored
     * @param file   The file to index, or the directory to recurse into to find files to index
     */
    private void indexDocs(IndexWriter writer, File file) {
        // do not try to index files that cannot be read
        if (!file.canRead()) {
            System.err.println("Skipped file, can't read: " + file);
            return;
        }

        // directories, recursive index
        if (file.isDirectory()) {
            String[] subfiles = file.list();
            // an IO error could occur
            if (subfiles != null) {
                for (String subfile : subfiles) {
                    indexDocs(writer, new File(file, subfile));
                }
            }
            return;
        }

        // index file content
        try (FileInputStream fis = new FileInputStream(file)) {

            // make a new, empty document
            Document doc = new Document();

            // parse File properties
            parseFileProperties(file, doc);

            // parse File Content
            try {
                // try extended
                parseFileContent(fis, doc);
            } catch (Exception e) {
                // error try normal
                parseFileContent_basic(fis, doc);
            }

            // save
            if (create) {
                // New index, so we just add the document (no old document can be there):
                System.out.println("adding " + file);
                writer.addDocument(doc);
            } else {
                // Existing index (an old copy of this document may have been indexed) so
                // we use updateDocument instead to replace the old one matching the exact
                // path, if present:
                System.out.println("updating " + file);
                writer.updateDocument(new Term("path", file.getPath()), doc);
            }

            // debug fields
            if (debug) {
                doc.getFields().forEach(f -> System.out.println("    " + f.name() + ": " + f.toString()));
            }

        } catch (Exception e) {
            System.err.println("Skipped file, can't index: " + file);
            e.printStackTrace();
        }

    }

    private void parseFileProperties(File file, Document doc) {
        // Add the path of the file as a field named "path".  Use a
        // field that is indexed (i.e. searchable), but don't tokenize
        // the field into separate words and don't index term frequency
        // or positional information:
        Field pathField = new StringField("path", file.getPath(), Field.Store.YES);
        doc.add(pathField);

        // Add the last modified date of the file a field named "modified".
        // Use a StoredField to return later its value as a response to a query.
        // This indexes to milli-second resolution, which
        // is often too fine.  You could instead create a number based on
        // year/month/day/hour/minutes/seconds, down the resolution you require.
        // For example the long value 2011021714 would mean
        // February 17, 2011, 2-3 PM.
        DateFormat dtFormat = SimpleDateFormat.getDateTimeInstance();
        doc.add(new StoredField("modified", dtFormat.format(file.lastModified())));
    }

    private void parseFileContent_basic(FileInputStream fis, Document doc) throws UnsupportedEncodingException {
        doc.add(new TextField("content", new BufferedReader(new InputStreamReader(fis, "UTF-8"))));
    }

    private void parseFileContent(FileInputStream fis, Document doc) throws ParserConfigurationException, IOException, SAXException {

        // parse XML dom
        org.w3c.dom.Document xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fis);

        // add TextField (long fields)
        for (String tag : new String[]{"description"}) {
            NodeList list = xmlDoc.getElementsByTagName("dc:" + tag);
            for (int i = 0; i < list.getLength(); i++) {
                doc.add(new TextField(tag, new StringReader(list.item(i).getTextContent())));
            }
        }

        // add StringField (short fields)
        for (String tag : new String[]{"contributor", "creator", "language", "publisher", "subject", "title", "type", "relation", "rights", "identifier"}) {
            NodeList list = xmlDoc.getElementsByTagName("dc:" + tag);
            for (int i = 0; i < list.getLength(); i++) {
                doc.add(new StringField(tag, list.item(i).getTextContent(), Field.Store.YES));
            }
        }

//        // add Geographical coordinates
//        NodeList lowerCorner = xmlDoc.getElementsByTagName("ows:LowerCorner");
//        NodeList upperCorner = xmlDoc.getElementsByTagName("ows:UpperCorner");
//        for (int i = 0; i < Math.min(lowerCorner.getLength(), upperCorner.getLength()); ++i) {
//            String[] xMin_yMin = lowerCorner.item(i).getTextContent().split(" ");
//            String[] xMax_yMax = upperCorner.item(i).getTextContent().split(" ");
//
//            doc.add(new DoublePoint(WEST, Double.parseDouble(xMin_yMin[0])));
//            doc.add(new DoublePoint(EAST, Double.parseDouble(xMax_yMax[0])));
//            doc.add(new DoublePoint(SOUTH, Double.parseDouble(xMin_yMin[1])));
//            doc.add(new DoublePoint(NORTH, Double.parseDouble(xMax_yMax[1])));
//        }

        // add date elements
        for (String tag : new String[]{"date"}) {
            NodeList list = xmlDoc.getElementsByTagName("dc:" + tag);
            for (int i = 0; i < list.getLength(); i++) {
                // removes all non-digit elements
                doc.add(new StringField(tag, list.item(i).getTextContent().replaceAll("[^\\d]", ""), Field.Store.YES));
            }
        }

//        // add temporal element
//        // DOC: <dcterms:temporal>begin=2002-03-01; end=2004-08-31;</dcterms:temporal>
//        // INDEX: begin=20020301 end=20040831
//        NodeList list = xmlDoc.getElementsByTagName("dcterms:temporal");
//        for (int i = 0; i < list.getLength(); i++) {
//            Matcher p = Pattern.compile("begin=([^;]*);\\s*end=([^;]*);").matcher(list.item(i).getTextContent());
//            while (p.find()) {
//                // removes all non-digit elements
//                doc.add(new DoublePoint(BEGIN, Double.parseDouble(p.group(1).replaceAll("[^\\d]", ""))));
//                doc.add(new DoublePoint(END, Double.parseDouble(p.group(2).replaceAll("[^\\d]", ""))));
//            }
//        }
    }

}
