import java.util.Arrays;

public class Tests {

    private static SearchFiles app;

    public static void main(String[] args) throws Exception {
        app = new SearchFiles();

        // spatial simple
        test("spatial:-180.0,180.0,-90.0,90.0", "12,13,14,15,16,17,18,19,20,21,23,24,25,26,27,28,29,30");
        test("spatial:-15.6,6.6,35.0,44.7", "12,13,14,15,16,17,18,19,20,21,23,26,29,30");
        test("spatial:-15.6,6.6,50.0,72.0", "12,16,17,18,23,26,27");
        test("spatial:-135.0,-110.0,50.0,72.0", "25,26,27,28");

        // spatial compounds
        test("spatial:-180.0,180.0,-90.0,90.0 title:natura", "17,12,13,14,15,16,18,19,20,21,23,24,25,26,27,28,29,30");
        test("spatial:-180.0,180.0,-90.0,90.0 title:nacional", "12,13,14,15,16,17,18,19,20,21,23,24,25,26,27,28,29,30,01,05");

        // temporal
        test("issued:[1980 TO 2010]", "01,02,03,04,05,06,07,08,09,10,11,13,18,19,20,23,24,25,27,29,30");
        test("created:[1980 TO 2010]", "14,15,16,17");
        test("issued:[1980  TO  2010] created:[1980 TO 2010]", "01,02,03,04,05,06,07,08,09,10,11,13,14,15,16,17,18,19,20,23,24,25,27,29,30");
        test("issued:[19890101 TO 19950101]", "01,02,20,24,27");
        test("issued:19940101", "01");

        System.out.println();
        System.out.println("All correct");
    }

    static void test(String query, String expectedResult) throws Exception {
        assertEqual(app.search(query), expectedResult.split(","));
    }

    static void assertEqual(Object[] obtained, Object[] expected) {
        if (!Arrays.equals(obtained, expected)) {
            System.err.println("[Asertion failed] Expected " + Arrays.toString(expected) + " but obtained " + Arrays.toString(obtained));
        } else {
            System.out.print(".");
        }
    }
}
