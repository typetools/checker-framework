package org.checkerframework.framework.test.diagnostics;

import java.util.List;
import org.checkerframework.framework.util.PluginUtil;

/** Represents an entire line of TestDiagnostics which is essentially a list of diagnostics */
public class TestDiagnosticLine {
    private final String filename;
    private final long lineNumber;
    private final String originalLine;
    private final List<TestDiagnostic> diagnostics;

    public TestDiagnosticLine(
            String filename,
            long lineNumber,
            String originalLine,
            List<TestDiagnostic> diagnostics) {
        this.filename = filename;
        this.lineNumber = lineNumber;
        this.originalLine = originalLine;
        this.diagnostics = diagnostics;
    }

    public String getFilename() {
        return filename;
    }

    public boolean hasDiagnostics() {
        return !diagnostics.isEmpty();
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public String getOriginalLine() {
        return originalLine;
    }

    /**
     * @return a String representation of how this diagnostic should appear in source. This may
     *     differ from the original line if there was no original line, the original line had
     *     extraneous whitespace.
     */
    public String asSourceString() {
        return "//:: "
                + PluginUtil.join(" :: ", TestDiagnosticUtils.diagnosticsToString(diagnostics));
    }

    public List<TestDiagnostic> getDiagnostics() {
        return diagnostics;
    }
}
