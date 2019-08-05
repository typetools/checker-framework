package org.checkerframework.framework.test.diagnostics;

import java.util.LinkedHashMap;
import java.util.Map;

/** Indicates what type of Error was, or expected to be, encountered during typechecking. */
public enum DiagnosticKind {
    Warning("warning"),
    Error("error"),
    Other("other");

    /** How the diagnostic identifier actually appears in error messages or source code. */
    public final String parseString;

    DiagnosticKind(String parseString) {
        this.parseString = parseString;
    }

    private static final Map<String, DiagnosticKind> stringToCategory = new LinkedHashMap<>();

    static {
        for (DiagnosticKind cat : values()) {
            stringToCategory.put(cat.parseString, cat);
        }
    }

    /**
     * Convert a string as it would appear in error messages or source code into a DiagnosticKind
     */
    public static DiagnosticKind fromParseString(String parseStr) {
        return stringToCategory.get(parseStr);
    }
}
