package tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Parses program arguments
 */
public class ArgsParser {

    private final List<Parameter> required = new ArrayList<>();
    private final List<Parameter> optional = new ArrayList<>();
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
     * @param name        The name of the parameter, as it should appear in the arguments
     * @param description the description of the parameter, for messages
     * @param amount      number of following arguments this parameter requires
     * @param listener    when the parameter is found, this consumer is called with the specified params
     */
    public ArgsParser addRequired(String name, String description, int amount, Consumer<List<String>> listener) {
        required.add(new Parameter(name, description, amount, listener));
        return this;
    }

    /**
     * Adds a new optional parameter. If not found nothing happens.
     *
     * @param name        The name of the parameter, as it should appear in the arguments
     * @param description the description of the parameter, for messages
     * @param amount      number of following arguments this parameter requires
     * @param listener    when the parameter is found, this consumer is called with the specified params
     */
    public ArgsParser addOptional(String name, String description, int amount, Consumer<List<String>> listener) {
        optional.add(new Parameter(name, description, amount, listener));
        return this;
    }

    /**
     * Starts the parsing process with the provided arguments
     */
    public void parse(String[] args) {
        List<String> l_args = new ArrayList<>(Arrays.asList(args));

        // get all
        List<Parameter> all = new ArrayList<>(required.size() + optional.size());
        all.addAll(required);
        all.addAll(optional);

        // for each arg
        nextArg:
        while (!l_args.isEmpty()) {
            String arg = l_args.remove(0);

            // check help
            if (arg.equals("-h") || arg.equals("--help"))
                exit(null, 0);

            // match parameter
            for (Iterator<Parameter> iterator = all.iterator(); iterator.hasNext(); ) {
                Parameter parameter = iterator.next();

                if (arg.equals(parameter.name)) {
                    // parameter found
                    if (l_args.size() < parameter.amount)
                        // no next parameter
                        exit("Parameter " + arg + " needs " + parameter.amount + " elements after it", 1);

                    // save
                    List<String> subArgs = l_args.subList(0, parameter.amount);
                    parameter.value.accept(subArgs);
                    subArgs.clear();
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
                    "Missing parameters: " + all.stream().map(v -> v.name).collect(Collectors.joining(", ")),
                    3
            );
        }
    }

    // ------------------------- private -------------------------

    private void exit(String reason, int status) {
        System.out.println("Usage: java " + getMainName() + " " +
                required.stream().map(v -> v.name + " " + IntStream.range(1, v.amount + 1).mapToObj(i -> "{param" + i + "}").collect(Collectors.joining(" "))).collect(Collectors.joining(" ")) + " " +
                optional.stream().map(v -> "[" + v.name + " " + IntStream.range(1, v.amount + 1).mapToObj(i -> "{param" + i + "}").collect(Collectors.joining(" ")) + "]").collect(Collectors.joining(" "))
        );
        System.out.println(description);
        required.forEach(v -> System.out.println("\t" + v.name + ": " + v.description));
        optional.forEach(v -> System.out.println("\t" + v.name + ": (optional) " + v.description));
        if (reason != null)
            System.err.println(reason);
        System.exit(status);
    }

    private String getMainName() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        return stack[stack.length - 1].getClassName();
    }


    // ------------------------- data -------------------------

    private static class Parameter {
        private final String name;
        private final String description;
        private final int amount;
        private final Consumer<List<String>> value;

        public Parameter(String name, String description, int amount, Consumer<List<String>> value) {
            this.name = name;
            this.description = description;
            this.amount = amount;
            this.value = value;
        }
    }
}
