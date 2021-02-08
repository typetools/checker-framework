package org.checkerframework.framework.test.diagnostics;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents an expected error/warning message in a Java test file or an error/warning reported by
 * the Javac compiler. By contrast, {@link TestDiagnosticLine} represents a set of TestDiagnostics,
 * all of which were read from the same line of a file.
 *
 * @see JavaDiagnosticReader
 */
public class TestDiagnostic {

    private final String filename;
    private final long lineNumber;
    private final DiagnosticKind kind;

    /**
     * An error key or full error message that usually appears between parentheses in diagnostic
     * messages.
     */
    private final String message;

    /** Whether this diagnostic should no longer be reported after whole program inference. */
    private final boolean isFixable;

    /**
     * Whether or not the toString representation should omit the parentheses around the message.
     */
    private final boolean omitParentheses;

    /** Basic constructor that sets the immutable fields of this diagnostic. */
    public TestDiagnostic(
            String filename,
            long lineNumber,
            DiagnosticKind kind,
            String message,
            boolean isFixable,
            boolean omitParentheses) {
        this.filename = filename;
        this.lineNumber = lineNumber;
        this.kind = kind;
        this.message = message;
        this.isFixable = isFixable;
        this.omitParentheses = omitParentheses;
    }

    public String getFilename() {
        return filename;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public DiagnosticKind getKind() {
        return kind;
    }

    public String getMessage() {
        return message;
    }

    public boolean isFixable() {
        return isFixable;
    }

    /**
     * Returns whether or not the printed representation should omit parentheses around the message.
     *
     * @return whether or not the printed representation should omit parentheses around the message
     */
    public boolean shouldOmitParentheses() {
        return omitParentheses;
    }

    /**
     * Equality is compared without isFixable/omitParentheses.
     *
     * @return true if this and otherObj are equal according to filename, lineNumber, kind, and
     *     message
     */
    @Override
    public boolean equals(@Nullable Object otherObj) {
        if (otherObj == null || otherObj.getClass() != TestDiagnostic.class) {
            return false;
        }

        final TestDiagnostic other = (TestDiagnostic) otherObj;
        return other.filename.equals(this.filename)
                && other.lineNumber == lineNumber
                && other.kind == this.kind
                && other.message.equals(this.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, lineNumber, kind, message);
    }

    /**
     * Returns a representation of this diagnostic as if it appeared in a diagnostics file.
     *
     * @return a representation of this diagnostic as if it appeared in a diagnostics file
     */
    @Override
    public String toString() {
        if (kind == DiagnosticKind.JSpecify) {
            return filename + ":" + lineNumber + ": " + message;
        }
        if (omitParentheses) {
            return filename + ":" + lineNumber + ": " + kind.parseString + ": " + message;
        }
        return filename + ":" + lineNumber + ": " + kind.parseString + ": (" + message + ")";
    }
}
