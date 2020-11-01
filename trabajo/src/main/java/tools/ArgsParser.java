package tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Parses program arguments
 */
public class ArgsParser {

    private final List<Element> required = new ArrayList<>();
    private final List<Element> optional = new ArrayList<>();
    private final String description;

    // ------------------------- public -------------------------

    /**
     * Initializes a new parser with the provided description
     */
    public ArgsParser(String description) {
        this.description = description;
    }

    /**
     * Adds a new required parameter. If not found an error is raised.
     *
     * @param param       The name of the parameter, as it should appear in the arguments
     * @param description the description of the parameter, for messages
     * @param value       if "param foo" is found, this consumer is called with 'foo'
     */
    public ArgsParser addRequired(String param, String description, Consumer<String> value) {
        required.add(new Element(param, description, value));
        return this;
    }

    /**
     * Adds a new optional parameter. If not found nothing happens.
     *
     * @param param       The name of the parameter, as it should appear in the arguments
     * @param description the description of the parameter, for messages
     * @param value       if "param foo" is found, this consumer is called with 'foo'
     */
    public ArgsParser addOptional(String param, String description, Consumer<String> value) {
        optional.add(new Element(param, description, value));
        return this;
    }

    /**
     * Starts the parsing process with the provided arguments
     */
    public void parse(String[] args) {

        // get all
        List<Element> all = new ArrayList<>(required.size() + optional.size());
        all.addAll(required);
        all.addAll(optional);

        // for each arg
        nextArg:
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            // check help
            if (arg.equals("-h") || arg.equals("--help"))
                exit(null, 0);

            // match parameter
            for (Iterator<Element> iterator = all.iterator(); iterator.hasNext(); ) {
                Element element = iterator.next();

                if (arg.equals(element.param)) {
                    // parameter found
                    i++;
                    if (i >= args.length)
                        // no next parameter
                        exit("Parameter " + arg + " needs an element after it", 1);

                    // save
                    element.value.accept(args[i]);
                    iterator.remove();
                    continue nextArg;
                }
            }

            // element not found
            exit("Unexpected argument \"" + arg + "\"", 2);
        }

        // remove optional
        all.removeAll(optional);

        // missing parameters
        if (!all.isEmpty()) {
            exit(
                    "Missing parameters: " + all.stream().map(v -> v.param).collect(Collectors.joining(", ")),
                    3
            );
        }
    }

    // ------------------------- private -------------------------

    private void exit(String reason, int status) {
        System.out.println("Usage: java " + getMainName() + " " +
                required.stream().map(v -> v.param + " {value}").collect(Collectors.joining(" ")) + " " +
                optional.stream().map(v -> "[" + v.param + " {value}]").collect(Collectors.joining(" "))
        );
        System.out.println(description);
        required.forEach(v -> System.out.println("\t" + v.param + ": " + v.description));
        optional.forEach(v -> System.out.println("\t" + v.param + ": (optional) " + v.description));
        if (reason != null)
            System.err.println(reason);
        System.exit(status);
    }

    private String getMainName() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        return stack[stack.length - 1].getClassName();
    }


    // ------------------------- data -------------------------

    private static class Element {
        private final String param;
        private final String description;
        private final Consumer<String> value;

        public Element(String param, String description, Consumer<String> value) {
            this.param = param;
            this.description = description;
            this.value = value;
        }
    }
}
