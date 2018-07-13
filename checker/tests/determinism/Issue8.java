package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

class Issue8 {
    /**
     * Splits the argument string into an array of tokens (command-line flags and arguments),
     * respecting single and double quotes.
     *
     * <p>This method is only appropriate when the {@code String[]} version of the arguments is not
     * available &mdash; for example, for the {@code premain} method of a Java agent.
     *
     * @param args the command line to be tokenized
     * @return a string array analogous to the argument to {@code main}.
     */
    // TODO: should this throw some exceptions?
    //    public static @PolyDet String @PolyDet [] tokenize(@PolyDet String args) {
    //
    //        // Split the args string on whitespace boundaries accounting for quoted
    //        // strings.
    //        args = args.trim();
    //        List<String> argList = new ArrayList<String>();
    //        String arg = "";
    //        char activeQuote = 0;
    //        for (int ii = 0; ii < args.length(); ii++) {
    //            char ch = args.charAt(ii);
    //            if ((ch == '\'') || (ch == '"')) {
    //                arg += ch;
    //                ii++;
    //                while ((ii < args.length()) && (args.charAt(ii) != ch)) {
    //                    arg += args.charAt(ii++);
    //                }
    //                arg += ch;
    //            } else if (Character.isWhitespace(ch)) {
    //                // System.out.printf ("adding argument '%s'%n", arg);
    //                argList.add(arg);
    //                arg = "";
    //                while ((ii < args.length()) && Character.isWhitespace(args.charAt(ii))) {
    //                    ii++;
    //                }
    //                if (ii < args.length()) {
    //                    // Encountered a non-whitespace character.
    //                    // Back up to process it on the next loop iteration.
    //                    ii--;
    //                }
    //            } else { // must be part of current argument
    //                arg += ch;
    //            }
    //        }
    //        if (!arg.equals("")) {
    //            argList.add(arg);
    //        }
    //
    //        String [] temp = new String [argList.size()];
    //        String [] argsArray = argList.toArray(temp);
    //        return argsArray;
    //    }

    public static @PolyDet List<@PolyDet String> copyList(@PolyDet List<@PolyDet String> strings) {
        List<String> copy = new ArrayList<String>();
        for (String s : strings) {
            copy.add(s);
        }
        return copy;
    }

    //    public static @PolyDet List<@PolyDet String> copyList1(@PolyDet List<@PolyDet String>
    // strings, @PolyDet List<@PolyDet String> copy) {
    //        for (String s : strings) {
    //            copy.add(s);
    //        }
    //        return copy;
    //    }
}
