package indexfiles.saver;


import indexfiles.parser.BasicParser;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;
import tools.CustomAnalyzer;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Manages the Lucene index, allow to create and/or update
 */
public class Saver {

    private final boolean create;
    private final boolean debug;
    private final IndexWriter writer;

    /**
     * @param indexPath path where the index will be created/updated
     * @param create    true to create, false to update
     * @param debug     if true, fields of each file will be printed
     */
    public Saver(String indexPath, boolean create, boolean debug) throws IOException {
        // parameters
        this.create = create;
        this.debug = debug;

        // initialize
        IndexWriterConfig iwc = new IndexWriterConfig(new CustomAnalyzer(true));
        if (create) {
            // Create a new index in the directory, removing any
            // previously indexed documents:
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        } else {
            // Add new documents to an existing index:
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        }
        writer = new IndexWriter(FSDirectory.open(Paths.get(indexPath)), iwc);

        // Optional: for better indexing performance, if you
        // are indexing many documents, increase the RAM
        // buffer.  But if you do this, increase the max heap
        // size to the JVM (eg add -Xmx512m or -Xmx1g):
        //
        // iwc.setRAMBufferSizeMB(256.0);
    }

    public void close() {
        // NOTE: if you want to maximize search performance,
        // you can optionally call forceMerge here.  This can be
        // a terribly costly operation, so generally it's only
        // worth it when your index is relatively static (ie
        // you're done adding documents to it):
        //
        // writer.forceMerge(1);

        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds the document to the index
     */
    public void addDoc(Document doc) {
        try {
            String path = doc.get(BasicParser.FIELD_PATH);

            // save
            if (create) {
                // New index, so we just add the document (no old document can be there):
                System.out.println("Adding " + path);
                writer.addDocument(doc);
            } else {
                // Existing index (an old copy of this document may have been indexed) so
                // we use updateDocument instead to replace the old one matching the exact
                // path, if present:
                System.out.println("Updating " + path);
                writer.updateDocument(new Term(BasicParser.FIELD_PATH, path), doc);
            }

            // debug fields
            if (debug) {
                doc.getFields().forEach(f -> System.out.println("    " + f.name() + ": " + f.toString()));
            }

        } catch (Exception e) {
            // error
            System.err.println("Skipped doc, can't save: " + doc);
            e.printStackTrace();
        }
    }

}
