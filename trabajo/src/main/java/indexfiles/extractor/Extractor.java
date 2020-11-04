package indexfiles.extractor;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts files to index
 */
public class Extractor {

    private final List<File> files = new ArrayList<>();

    /**
     * Indexes the given file, or if a directory is given,
     * recurses over files and directories found under the given directory.
     */
    public void indexPath(String path) {
        File docDir = new File(path);
        if (!docDir.exists() || !docDir.canRead()) {
            System.out.println("Document directory '" + docDir.getAbsolutePath() + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }
        indexFile(docDir);
    }

    /**
     * Indexes the given file, or if a directory is given,
     * recurses over files and directories found under the given directory.
     */
    public void indexFile(File file) {
        // do not try to index files that cannot be read
        if (!file.canRead()) {
            System.err.println("Skipped file, can't read: " + file);
            return;
        }

        // directories, recursive index
        if (file.isDirectory()) {
            System.out.println("Listing " + file);
            File[] subfiles = file.listFiles();
            if (subfiles != null) {
                for (File subfile : subfiles) {
                    indexFile(subfile);
                }
            }
            return;
        }

        // valid file
        files.add(file);
    }

    /**
     * @return the indexed files
     */
    public List<File> getFiles() {
        return files;
    }


}
