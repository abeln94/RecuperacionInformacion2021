import java.util.Arrays;

public class TestSearchFiles {
    public static void main(String[] args) throws Exception {
        SearchFiles app = new SearchFiles();

        // simple
        assertEqual(app.search("spatial:-180.0,180.0,-90.0,90.0"), "12,13,14,15,16,17,18,19,20,21,23,24,25,26,27,28,29,30".split(","));
        assertEqual(app.search("spatial:-15.6,6.6,35.0,44.7"), "12,13,14,15,16,17,18,19,20,21,23,26,29,30".split(","));
        assertEqual(app.search("spatial:-15.6,6.6,50.0,72.0"), "12,16,17,18,23,26,27".split(","));
        assertEqual(app.search("spatial:-135.0,-110.0,50.0,72.0"), "25,26,27,28".split(","));

        // compounds
        assertEqual(app.search("spatial:-180.0,180.0,-90.0,90.0 title:natura"), "17,12,13,14,15,16,18,19,20,21,23,24,25,26,27,28,29,30".split(","));
        assertEqual(app.search("spatial:-180.0,180.0,-90.0,90.0 title:nacional"), "12,13,14,15,16,17,18,19,20,21,23,24,25,26,27,28,29,30,01,05".split(","));

        System.out.println("all correct");
    }

    static void assertEqual(Object[] obtained, Object[] expected) {
        if (!Arrays.equals(obtained, expected)) {
            System.err.println("[Asertion failed] Expected " + Arrays.toString(expected) + " but obtained " + Arrays.toString(obtained));
        } else {
            System.out.print(".");
        }
    }
}
