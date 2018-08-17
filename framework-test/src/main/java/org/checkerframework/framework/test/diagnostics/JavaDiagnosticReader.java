package org.checkerframework.framework.test.diagnostics;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.tools.JavaFileObject;

/**
 * A file can indicate expected javac diagnostics. There are two types of such files: Java source
 * files, and Diagnostic files.
 *
 * <p>This class contains a static method to read each type of file. The output of each is a list of
 * TestDiagnostic.
 */
public class JavaDiagnosticReader implements Iterator<TestDiagnosticLine> {

    // This class begins with the most common static helper methods that are used to read
    // diagnostics

    /**
     * Returns all the diagnostics in any of the files.
     *
     * @param files the Java files to read; each is a File or a JavaFileObject
     * @return the List of TestDiagnostics from the input file
     */
    // The argument is has type Iterable<? extends Object> because Java cannot resolve the overload
    // of two versions that take Iterable<? extends File> and Iterable<? extends JavaFileObject>.
    public static List<TestDiagnostic> readJavaSourceFiles(Iterable<? extends Object> files) {
        List<JavaDiagnosticReader> readers = new ArrayList<>();
        for (Object file : files) {
            if (file instanceof JavaFileObject) {
                readers.add(new JavaDiagnosticReader((JavaFileObject) file, JAVA_COMMENT_CODEC));
            } else if (file instanceof File) {
                readers.add(new JavaDiagnosticReader((File) file, JAVA_COMMENT_CODEC));
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
     * Reads diagnostics line-by-line from the input Diagnostic files.
     *
     * @param files a set of Diagnostic Files
     * @return the List of TestDiagnosticLines from the input files
     */
    public static List<TestDiagnostic> readDiagnosticFiles(Iterable<? extends File> files) {
        List<JavaDiagnosticReader> readers = new ArrayList<>();
        for (File file : files) {
            readers.add(new JavaDiagnosticReader(file, DIAGNOSTIC_FILE_CODEC));
        }
        return readDiagnostics(readers);
    }

    ///
    /// end of public static methods, start of private static methods
    ///

    /**
     * Returns all the diagnostics in any of the files.
     *
     * @param file the file (Java or Diagnostics format) to read
     * @param codec a codec corresponding to the file type being read
     * @return the List of TestDiagnosticLines from the input file
     */
    private static List<TestDiagnostic> readDiagnostics(Iterable<JavaDiagnosticReader> readers) {
        return getDiagnostics(readDiagnosticLines(readers));
    }

    /**
     * Reads the entire input file using the given codec and returns the resulting line.
     *
     * @param file the file (Java or Diagnostics format) to read
     * @param codec a codec corresponding to the file type being read
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
     * Reads the entire input file using the given codec and returns the resulting line.
     *
     * @param file the file (Java or Diagnostics format) to read
     * @param codec a codec corresponding to the file type being read
     * @return the List of TestDiagnosticLines from the input file
     */
    private static List<TestDiagnosticLine> readDiagnosticLines(JavaDiagnosticReader reader) {
        List<TestDiagnosticLine> diagnosticLines = new ArrayList<>();
        while (reader.hasNext()) {
            TestDiagnosticLine line = reader.next();
            if (line.hasDiagnostics()) {
                diagnosticLines.add(line);
            }
        }
        reader.close();

        return diagnosticLines;
    }

    /** Converts a list of TestDiagnosticLine into a list of TestDiagnostic. */
    private static List<TestDiagnostic> getDiagnostics(List<TestDiagnosticLine> lines) {
        List<TestDiagnostic> result = new ArrayList<TestDiagnostic>((int) (lines.size() * 1.1));
        for (TestDiagnosticLine line : lines) {
            result.addAll(line.getDiagnostics());
        }
        return result;
    }

    /**
     * DiagnosticCodec converts a line of a file into a TestDiagnosticLine. There are currently two
     * possible formats: one for Java source code, and one for Diagnostic files.
     */
    private interface DiagnosticCodec {
        public TestDiagnosticLine convertLine(String filename, long lineNumber, String line);
    }

    /** Parses a string that was written as a comment in a Java file. */
    private static final DiagnosticCodec JAVA_COMMENT_CODEC =
            new DiagnosticCodec() {
                @Override
                public TestDiagnosticLine convertLine(
                        String filename, long lineNumber, String line) {
                    return TestDiagnosticUtils.fromJavaSourceLine(filename, line, lineNumber);
                }
            };

    /** Parses a string that was written as a line in a Diagnostic File. */
    private static final DiagnosticCodec DIAGNOSTIC_FILE_CODEC =
            new DiagnosticCodec() {
                @Override
                public TestDiagnosticLine convertLine(
                        String filename, long lineNumber, String line) {
                    return TestDiagnosticUtils.fromDiagnosticFileLine(line);
                }
            };

    ///
    /// End of static methods, start of per-instance state
    ///

    private final File toRead;
    private final JavaFileObject toReadFileObject;
    private final DiagnosticCodec codec;

    private final String filename;

    private boolean initialized = false;
    private boolean closed = false;

    private LineNumberReader reader = null;

    private String nextLine = null;
    private int nextLineNumber = -1;

    private JavaDiagnosticReader(File toRead, DiagnosticCodec codec) {
        this.toRead = toRead;
        this.toReadFileObject = null;
        this.codec = codec;
        this.filename = shortFileName(toRead.getAbsolutePath());
    }

    private JavaDiagnosticReader(JavaFileObject toRead, DiagnosticCodec codec) {
        this.toRead = null;
        this.toReadFileObject = toRead;
        this.codec = codec;
        this.filename = shortFileName(toRead.getName());
    }

    private String shortFileName(String name) {
        int index = name.lastIndexOf(File.separator);
        return name.substring(index + 1, name.length());
    }

    private void init() throws IOException {
        if (!initialized && !closed) {
            initialized = true;

            Reader fileReader =
                    (toRead != null) ? new FileReader(toRead) : toReadFileObject.openReader(true);
            reader = new LineNumberReader(fileReader);
            advance();
        }
    }

    @Override
    public boolean hasNext() {
        if (closed) {
            return false;
        }

        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return nextLine != null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException(
                "Cannot remove elements using JavaDiagnosticFileReader.");
    }

    @Override
    public TestDiagnosticLine next() {
        try {
            init();

            if (nextLine == null) {
                throw new NoSuchElementException();
            } else if (closed) {
                throw new RuntimeException("Reader has been closed: " + toRead.getAbsolutePath());
            }

            String current = nextLine;
            int currentLineNumber = nextLineNumber;

            advance();

            if (TestDiagnosticUtils.isJavaDiagnosticLineStart(current)) {
                while (TestDiagnosticUtils.isJavaDiagnosticLineContinuation(nextLine)) {
                    current = current.trim() + " " + TestDiagnosticUtils.continuationPart(nextLine);
                    currentLineNumber = nextLineNumber;
                    advance();
                }
            }

            if (nextLine == null) {
                close();
            }

            return codec.convertLine(filename, currentLineNumber, current);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void advance() throws IOException {
        nextLine = reader.readLine();
        nextLineNumber = reader.getLineNumber();
    }

    public void close() {
        try {
            if (initialized) {
                reader.close();
            }

            closed = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
