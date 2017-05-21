package org.checkerframework.eclipse.prefs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.eclipse.util.PluginUtil;

public class OptionLine {

    private final String arg;
    private final boolean active;

    private static final Pattern DECODE_PATTERN =
            Pattern.compile(
                    "^OptionLine\\(\"(.*)\", ("
                            + Boolean.toString(true)
                            + "|"
                            + Boolean.toString(false)
                            + ")\\)$");

    public OptionLine(final String serialized) {
        assert serialized != null : "OptionLine constructor param can not be null!";

        final Matcher matcher = DECODE_PATTERN.matcher(serialized);
        if (!matcher.matches()) {
            throw new RuntimeException(
                    "OptionLine single string constructor parameter is malformed!  "
                            + "serialized = "
                            + serialized);
        }

        this.arg = matcher.group(1);
        this.active = Boolean.parseBoolean(matcher.group(2));
    }

    public OptionLine(final String arg, final boolean active) {
        this.arg = arg;
        this.active = active;
    }

    public String getArgument() {
        return arg;
    }

    public boolean isActive() {
        return active;
    }

    public String toString() {
        return "OptionLine(\"" + arg + "\"" + ", " + Boolean.toString(active) + ")";
    }

    public static final String optionLinesToCmdLine(final Collection<OptionLine> lines) {
        String cmdLine = "";

        boolean addSpace = false;

        for (final OptionLine line : lines) {
            if (line.isActive()) {
                if (addSpace) {
                    cmdLine += " ";
                } else {
                    addSpace = true;
                }
            }
        }

        return cmdLine;
    }

    public static final List<OptionLine> parseOptions(final String option) {
        final List<OptionLine> options = new ArrayList<OptionLine>();

        if (option == null || option.isEmpty()) {
            return options;
        }

        if (option.startsWith("[")) {
            for (final String classDef : findClassDefs("OptionLine", option)) {
                options.add(new OptionLine(classDef));
            }
        } else { // Just make it one big option so people don't get interrupted
            options.add(new OptionLine(option.trim(), true));
        }

        return options;
    }

    // expected value format: [className(.*),className(.*),...,className(.*)]
    public static List<String> findClassDefs(final String className, final String value) {
        String remaining = value;
        final List<String> matches = new ArrayList<String>();

        int charPos = 0;
        if (remaining.startsWith("[") || !remaining.endsWith("]")) {
            remaining = remaining.substring(1).substring(0, remaining.length() - 2); // strip off []
        } else {
            throw new RuntimeException("Invalid option collection " + value);
        }

        final String classNameWithParen = className + "(";

        while (!remaining.isEmpty()) {
            if (remaining.charAt(0) == ',') {
                remaining = remaining.substring(1);
            }
            if (!remaining.substring(charPos).startsWith(classNameWithParen)) {
                throw new RuntimeException("Invalid option collection " + value);
            }
            remaining = remaining.substring(classNameWithParen.length());

            int open = 1;

            int index;
            for (index = 0; open > 0 && index < remaining.length(); index++) {
                final char c = remaining.charAt(index);
                switch (c) {
                    case '(':
                        ++open;
                        break;
                    case ')':
                        --open;
                        break;
                    default:
                        break;
                }
            }

            if (index == remaining.length() && open > 0) {
                throw new RuntimeException(
                        "Unbalanced parentheses in "
                                + value
                                + " starting at char: "
                                + (value.length() - remaining.length()));
            }

            matches.add("OptionLine(" + remaining.substring(0, index - 1) + ")");
            remaining = remaining.substring(index);
        }

        return matches;
    }

    public static final String optionLinesToString(final Collection<OptionLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return "[]";
        }

        return "[" + PluginUtil.join(",", lines) + "]";
    }
}
