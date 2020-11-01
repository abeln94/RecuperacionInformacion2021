package searchfiles.extractor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Extracts texts from the input console (asks to the user)
 */
public class ConsoleExtractor extends Extractor {

    private BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    private String line;

    @Override
    public boolean hasNext() {

        // prompt the user
        System.out.println("Enter query: ");

        // read from the user
        try {
            line = in.readLine();
        } catch (IOException ignored) {
            return false;
        }

        // check invalid
        if (line == null) {
            return false;
        }
        line = line.trim();
        if (line.length() == 0) {
            return false;
        }

        // all ok
        return true;
    }

    @Override
    public Element getNext() {
        return new Element("console", line);
    }
}
