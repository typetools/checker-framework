package org.checkerframework.framework.test.diagnostics;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.tools.JavaFileObject;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.checkerframework.dataflow.qual.Pure;

/**
 * This class reads expected javac diagnostics from a single file. Its implementation is as an
 * iterator over {@link TestDiagnosticLine}. However, clients should call the static methods: {@link
 * #readJavaSourceFiles} reads diagnostics from multiple Java source files, and {@link
 * #readDiagnosticFiles} reads diagnostics from multiple "diagnostic files".
 */
public class JavaDiagnosticReader implements Iterator<TestDiagnosticLine> {

    ///
    /// This class begins with the publc static methods that clients use to read diagnostics.
    ///

    /**
     * Returns all the diagnostics in any of the Java source files.
     *
     * @param files the Java files to read; each is a File or a JavaFileObject
     * @return the TestDiagnostics from the input file
     */
    // The argument has type Iterable<? extends Object> because Java cannot resolve the overload
    // of two versions that take Iterable<? extends File> and Iterable<? extends JavaFileObject>.
    public static List<TestDiagnostic> readJavaSourceFiles(Iterable<? extends Object> files) {
        List<JavaDiagnosticReader> readers = new ArrayList<>();
        for (Object file : files) {
            if (file instanceof JavaFileObject) {
                readers.add(
                        new JavaDiagnosticReader(
                                (JavaFileObject) file, TestDiagnosticUtils::fromJavaSourceLine));
            } else if (file instanceof File) {
                readers.add(
                        new JavaDiagnosticReader(
                                (File) file, TestDiagnosticUtils::fromJavaSourceLine));
            } else {
                throw new IllegalArgumentException(
                        String.format(
                                "Elements of argument should be File or JavaFileObject, not %s: %s",
                                file.getClass(), file));
            }
        }
        return readDiagnostics(readers);
    }

    /**
     * Reads diagnostics line-by-line from the input diagnostic files.
     *
     * @param files a set of diagnostic files
     * @return the TestDiagnosticLines from the input files
     */
    public static List<TestDiagnostic> readDiagnosticFiles(Iterable<? extends File> files) {
        List<JavaDiagnosticReader> readers = new ArrayList<>();
        for (File file : files) {
            readers.add(
                    new JavaDiagnosticReader(
                            file,
                            (filename, line, lineNumber) ->
                                    TestDiagnosticUtils.fromDiagnosticFileLine(line)));
        }
        return readDiagnostics(readers);
    }

    ///
    /// End of public static methods, start of private static methods.
    ///

    /**
     * Returns all the diagnostics in any of the files.
     *
     * @param readers the files (Java or Diagnostics format) to read
     * @return the List of TestDiagnosticLines from the input file
     */
    private static List<TestDiagnostic> readDiagnostics(Iterable<JavaDiagnosticReader> readers) {
        return diagnosticLinesToDiagnostics(readDiagnosticLines(readers));
    }

    /**
     * Reads the entire input file using the given codec and returns the resulting line.
     *
     * @param readers the files (Java or Diagnostics format) to read
     * @return the List of TestDiagnosticLines from the input file
     */
    private static List<TestDiagnosticLine> readDiagnosticLines(
            Iterable<JavaDiagnosticReader> readers) {
        List<TestDiagnosticLine> result = new ArrayList<>();
        for (JavaDiagnosticReader reader : readers) {
            result.addAll(readDiagnosticLines(reader));
        }
        return result;
    }

    /**
     * Reads the entire input file using the given codec and returns the resulting lines, filtering
     * out empty ones produced by JavaDiagnosticReader.
     *
     * @param reader the file (Java or Diagnostics format) to read
     * @return the List of TestDiagnosticLines from the input file
     */
    private static List<TestDiagnosticLine> readDiagnosticLines(JavaDiagnosticReader reader) {
        List<TestDiagnosticLine> diagnosticLines = new ArrayList<>();
        while (reader.hasNext()) {
            TestDiagnosticLine line = reader.next();
            // A JavaDiagnosticReader can return a lot of empty diagnostics.  Filter them out.
            if (line.hasDiagnostics()) {
                diagnosticLines.add(line);
            }
        }
        return diagnosticLines;
    }

    /** Converts a list of TestDiagnosticLine into a list of TestDiagnostic. */
    private static List<TestDiagnostic> diagnosticLinesToDiagnostics(
            List<TestDiagnosticLine> lines) {
        List<TestDiagnostic> result = new ArrayList<>((int) (lines.size() * 1.1));
        for (TestDiagnosticLine line : lines) {
            result.addAll(line.getDiagnostics());
        }
        return result;
    }

    /**
     * StringToTestDiagnosticLine converts a line of a file into a TestDiagnosticLine. There are
     * currently two possible formats: one for Java source code, and one for Diagnostic files.
     *
     * <p>No classes implement this interface. The methods TestDiagnosticUtils.fromJavaSourceLine
     * and TestDiagnosticUtils.fromDiagnosticFileLine instantiate the method.
     */
    private interface StringToTestDiagnosticLine {

        /**
         * Converts the specified line of the file into a {@link TestDiagnosticLine}.
         *
         * @param filename name of the file
         * @param line the text of the line to convert to a TestDiagnosticLine
         * @param lineNumber the line number of the line
         * @return TestDiagnosticLine corresponding to {@code line}
         */
        TestDiagnosticLine createTestDiagnosticLine(String filename, String line, long lineNumber);
    }

    ///
    /// End of static methods, start of per-instance state.
    ///

    private final StringToTestDiagnosticLine codec;

    private final String filename;

    private LineNumberReader reader;

    private @Nullable String nextLine = null;
    private @GTENegativeOne int nextLineNumber = -1;

    private JavaDiagnosticReader(File toRead, StringToTestDiagnosticLine codec) {
        this.codec = codec;
        this.filename = toRead.getName();
        try {
            reader = new LineNumberReader(new FileReader(toRead));
            advance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JavaDiagnosticReader(
            JavaFileObject toReadFileObject, StringToTestDiagnosticLine codec) {
        this.codec = codec;
        this.filename = new File(toReadFileObject.getName()).getName();
        try {
            reader = new LineNumberReader(toReadFileObject.openReader(true));
            advance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Pure
    public boolean hasNext() {
        return nextLine != null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException(
                "Cannot remove elements using JavaDiagnosticFileReader.");
    }

    @Override
    public TestDiagnosticLine next() {
        if (nextLine == null) {
            throw new NoSuchElementException();
        }

        String currentLine = nextLine;
        int currentLineNumber = nextLineNumber;

        try {
            advance();

            currentLine = TestDiagnosticUtils.handleEndOfLineJavaDiagnostic(currentLine);

            if (TestDiagnosticUtils.isJavaDiagnosticLineStart(currentLine)) {
                while (TestDiagnosticUtils.isJavaDiagnosticLineContinuation(nextLine)) {
                    currentLine =
                            currentLine.trim()
                                    + " "
                                    + TestDiagnosticUtils.continuationPart(nextLine);
                    currentLineNumber = nextLineNumber;
                    advance();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return codec.createTestDiagnosticLine(filename, currentLine, currentLineNumber);
    }

    @RequiresNonNull("reader")
    protected void advance(@UnknownInitialization JavaDiagnosticReader this) throws IOException {
        nextLine = reader.readLine();
        nextLineNumber = reader.getLineNumber();
        if (nextLine == null) {
            reader.close();
        }
    }
}
