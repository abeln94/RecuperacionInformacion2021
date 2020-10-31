package searcher;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Searches a query into the index
 */
public class Searcher {

    private final IndexSearcher searcher;

    /**
     * Constructor, provide the index path
     */
    public Searcher(String index) throws IOException {
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));

        searcher = new IndexSearcher(reader);
//        searcher.setSimilarity(new ClassicSimilarity()); // from the teacher
    }

    /**
     * Performs the search, returns the list of documents
     */
    public List<Document> search(Query query) throws IOException {
        ArrayList<Document> documents = new ArrayList<>();
        for (ScoreDoc doc : searcher.search(query, Integer.MAX_VALUE).scoreDocs) {
            documents.add(searcher.doc(doc.doc));
        }
        return documents;
    }
}
