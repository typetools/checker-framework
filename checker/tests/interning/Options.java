import org.checkerframework.checker.interning.qual.*;

import java.util.ArrayList;
import java.util.List;

// Test case lifted from plume.Options
public class Options {

    public void minimal(String s) {
        String arg = ""; // interned here
        @Interned String arg2 = arg;
        arg += s; // no longer interned
        // :: error: (assignment.type.incompatible)
        arg2 = arg;
    }

    public void minimal2(char c) {
        String arg = ""; // interned here
        @Interned String arg2 = arg;
        arg += c; // no longer interned
        // :: error: (assignment.type.incompatible)
        arg2 = arg;
    }

    public String[] otherparse(String args) {

        // Split the args string on whitespace boundaries accounting for quoted
        // strings.
        args = args.trim();
        List<String> arg_list = new ArrayList<>();
        String arg = "";
        char active_quote = 0;
        // for (int ii = 0; ii < args.length(); ii++) {
        char ch = args.charAt(0);
        // arg = arg + ch;

        // if ((ch == '\'') || (ch == '"')) {
        arg += ch;
        // }
        // }
        // :: error: (assignment.type.incompatible)
        @Interned String arg2 = arg;

        if (!arg.equals("")) {
            arg_list.add(arg);
        }

        String[] argsArray = arg_list.toArray(new String[arg_list.size()]);
        return null;
    }

    public String[] parse(String args) {

        // Split the args string on whitespace boundaries accounting for quoted
        // strings.
        args = args.trim();
        List<String> arg_list = new ArrayList<>();
        String arg = "";
        char active_quote = 0;
        for (int ii = 0; ii < args.length(); ii++) {
            char ch = args.charAt(ii);
            if ((ch == '\'') || (ch == '"')) {
                arg += ch;
                ii++;
                while ((ii < args.length()) && (args.charAt(ii) != ch)) {
                    arg += args.charAt(ii++);
                }
                arg += ch;
            } else if (Character.isWhitespace(ch)) {
                // System.out.printf ("adding argument '%s'%n", arg);
                arg_list.add(arg);
                arg = "";
                while ((ii < args.length()) && Character.isWhitespace(args.charAt(ii))) {
                    ii++;
                }
                if (ii < args.length()) {
                    ii--;
                }
            } else { // must be part of current argument
                arg += ch;
            }
        }
        // :: error: (assignment.type.incompatible)
        @Interned String arg2 = arg;

        if (!arg.equals("")) {
            arg_list.add(arg);
        }

        String[] argsArray = arg_list.toArray(new String[arg_list.size()]);
        return null;
    }
}
