package org.checkerframework.framework.test.diagnostics;

/**
 * Represents an expected error/warning message in a Java test file or an error/warning reported by
 * the Javac compiler. See {@link JavaDiagnosticReader} and {@link TestDiagnosticLine}.
 */
public class TestDiagnostic {

    private final long lineNumber;
    private final DiagnosticKind kind;
    private final String filename;

    /** Whether this diagnostic should no longer be reported after whole program inference */
    private final boolean isFixable;

    /**
     * An error key or full error message that usually appears between parentheses in diagnostic
     * messages
     */
    private final String message;

    /**
     * Whether or not String representation methods should omit the parentheses around the message
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

    public boolean isFixable() {
        return isFixable;
    }

    public String getMessage() {
        return message;
    }

    /**
     * @return whether or not the printed representation should omit parentheses around the message
     */
    public boolean shouldOmitParentheses() {
        return omitParentheses;
    }

    /**
     * @return a String representing the format of this diagnostic as if it appeared in a source
     *     file
     */
    public String asSourceString() {
        if (omitParentheses) {
            return kind.parseString + " " + message;
        }
        return kind.parseString + " (" + message + ")";
    }

    /**
     * Equality is compared without isFixable/omitParentheses
     *
     * @return true if this and otherObj are equal according to lineNumber, kind, and message
     */
    @Override
    public boolean equals(Object otherObj) {
        if (otherObj == null || !otherObj.getClass().equals(TestDiagnostic.class)) {
            return false;
        }

        final TestDiagnostic other = (TestDiagnostic) otherObj;
        return other.lineNumber == lineNumber
                && other.kind == this.kind
                && other.message.equals(this.message)
                && other.filename.equals(this.filename);
    }

    @Override
    public int hashCode() {
        return 331
                * ((int) lineNumber)
                * kind.hashCode()
                * message.hashCode()
                * filename.hashCode();
    }

    /** @return a representation of this diagnostic as if it appeared in a diagnostics file */
    @Override
    public String toString() {
        if (omitParentheses) {
            return filename + ":" + lineNumber + ": " + kind.parseString + ": " + message;
        }
        return filename + ":" + lineNumber + ": " + kind.parseString + ": (" + message + ")";
    }
}
