package indexfiles.indexer;

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

public class Indexer {

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
//            try {
            // try extended
            parseFileContent(fis, doc);
//            } catch (Exception e) {
//                // error try normal
//                parseFileContent_basic(fis, doc);
//            }

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
        for (String tag : new String[]{"creator","contributor","description", "publisher", "subject", "title", "relation", "rights", "identifier"}) {
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
        for (String tag : new String[]{"date"}) {
            NodeList list = xmlDoc.getElementsByTagName("dc:" + tag);
            for (int i = 0; i < list.getLength(); i++) {
                // removes all non-digit elements
                doc.add(new TextField(tag, list.item(i).getTextContent().replaceAll("[^\\d]", ""), Field.Store.YES));
            }
        }

    }

}
