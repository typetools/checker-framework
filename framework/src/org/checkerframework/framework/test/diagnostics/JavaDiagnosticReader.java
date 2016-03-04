package org.checkerframework.framework.test.diagnostics;

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Reads a file that has serialized javac diagnostics and returns either a list of TestDiagnosticLines or
 * TestDiagnostics.  This file might either:
 *    a) a Java file, which is read by creating a JavaDiagnosticReader with the JAVA_COMMENT_CODEC as
 *    b) A "Diagnostic" file, which is read by creating a JavaDiagnosticReader with a DIAGNOSTIC_FILE_CODEC
 */
public class JavaDiagnosticReader implements Iterator<TestDiagnosticLine> {

    //This class begins with the most common static helper methods that are used to read diagnostics

    /**
     * Reads the entire input file using the given codec and returns the resulting line.
     * @param toRead The file (Java or Diagnostics format) to read
     * @param codec A codec corresponding to the file type being read
     * @param omitEmptyDiagnostics Whether or not lines that do not contain any diagnostics should be
     *                             reported as empty TestDiagnosticLines
     * @return The List of TestDiagnosticLines from the input file
     */
    public static List<TestDiagnosticLine> readDiagnostics(File toRead, DiagnosticCodec codec, boolean omitEmptyDiagnostics) {
        List<TestDiagnosticLine> lines = new ArrayList<>();
        JavaDiagnosticReader reader = new JavaDiagnosticReader(toRead, codec);
        while (reader.hasNext()) {
            TestDiagnosticLine line = reader.next();
            if (!omitEmptyDiagnostics || line.hasDiagnostics()) {
                lines.add(line);
            }
        }
        reader.close();

        return lines;
    }

    /**
     * Reads diagnostic lines from the comments of the input Java file.
     * @param toRead A Java File
     * @param omitEmptyDiagnostics Whether or not lines that do not contain any diagnostics should be
     *                             reported as empty TestDiagnosticLines
     * @return The List of TestDiagnosticLines from the input file
     */
    public static List<TestDiagnosticLine> readDiagnostics(File toRead, boolean omitEmptyDiagnostics) {
        return readDiagnostics(toRead, JAVA_COMMENT_CODEC, omitEmptyDiagnostics);
    }

    /**
     * Reads diagnostic lines from the comments of a set of Java file.
     * @param toRead Java files to read using the JAVA_COMMENT_CODEC
     * @param omitEmptyDiagnostics Whether or not lines that do not contain any diagnostics should be
     *                             reported as empty TestDiagnosticLines
     * @return The List of TestDiagnosticLines from the input Jav afiles
     */
    public static List<TestDiagnosticLine> readDiagnosticLines(Iterable<File> toRead, boolean omitEmptyDiagnostics) {
        List<TestDiagnosticLine> lines = new ArrayList<>();
        for (File file : toRead) {
            lines.addAll(readDiagnostics(file, omitEmptyDiagnostics));
        }
        return lines;
    }

    /**
     * Reads diagnostics from the comments of a set of Java file.
     * @param toRead Java files to read using the JAVA_COMMENT_CODEC
     * @param omitEmptyDiagnostics Whether or not lines that do not contain any diagnostics should be
     *                             reported as empty TestDiagnosticLines
     * @return The List of TestDiagnostics (not lines) from the files ToRead
     */
    public static List<TestDiagnostic> readDiagnostics(Iterable<File> toRead, boolean omitEmptyDiagnostics) {
        List<TestDiagnosticLine> lines = readDiagnosticLines(toRead, omitEmptyDiagnostics);

        List<TestDiagnostic> diagnostics = new ArrayList<TestDiagnostic>((int) (lines.size() + lines.size() * 0.1));
        for (TestDiagnosticLine line : lines) {
            diagnostics.addAll(line.getDiagnostics());
        }
        return diagnostics;
    }

    /**
     * Reads diagnostic lines from the comments of a set of Java file.
     * @param toRead Java files to read using the JAVA_COMMENT_CODEC
     * @param omitEmptyDiagnostics Whether or not lines that do not contain any diagnostics should be
     *                             reported as empty TestDiagnosticLines
     * @return The List of TestDiagnostics (not lines) from the files ToRead
     */
    public static List<TestDiagnosticLine> readDiagnosticsJfo(JavaFileObject toRead, boolean omitEmptyDiagnostics) {
        List<TestDiagnosticLine> lines = new ArrayList<>();
        JavaDiagnosticReader reader = new JavaDiagnosticReader(toRead, JAVA_COMMENT_CODEC);
        while (reader.hasNext()) {
            TestDiagnosticLine line = reader.next();
            if (!omitEmptyDiagnostics || line.hasDiagnostics()) {
                lines.add(line);
            }
        }
        reader.close();

        return lines;
    }


    /**
     * Reads diagnostic lines from the comments of a set of Java file.
     * @param toRead Java files to read using the JAVA_COMMENT_CODEC
     * @param omitEmptyDiagnostics Whether or not lines that do not contain any diagnostics should be
     *                             reported as empty TestDiagnosticLines
     * @return The List of TestDiagnosticLines from the input Jav afiles
     */
    public static List<TestDiagnosticLine> readExpectedDiagnosticLinesJfo(Iterable<? extends JavaFileObject> toRead, boolean omitEmptyDiagnostics) {
        List<TestDiagnosticLine> lines = new ArrayList<>();
        for (JavaFileObject file : toRead) {
            lines.addAll(readDiagnosticsJfo(file, omitEmptyDiagnostics));
        }
        return lines;
    }


    /**
     * Reads diagnostics from the comments of a set of Java file.
     * @param toRead Java files to read using the JAVA_COMMENT_CODEC
     * @param omitEmptyDiagnostics Whether or not lines that do not contain any diagnostics should be
     *                             reported as empty TestDiagnosticLines
     * @return The List of TestDiagnostics (not lines)
     */
    public static List<TestDiagnostic> readExpectedDiagnosticsJfo(Iterable<? extends JavaFileObject> toRead, boolean omitEmptyDiagnostics) {
        List<TestDiagnosticLine> lines = readExpectedDiagnosticLinesJfo(toRead, omitEmptyDiagnostics);

        List<TestDiagnostic> diagnostics = new ArrayList<TestDiagnostic>((int) (lines.size() + lines.size() * 0.1));
        for (TestDiagnosticLine line : lines) {
            diagnostics.addAll(line.getDiagnostics());
        }
        return diagnostics;
    }

    /**
     * Reads diagnostic lines line-by-line from the input Diagnostic file.
     * @param toRead A Diagnostic File
     * @param omitEmptyDiagnostics Whether or not lines that do not contain any diagnostics should be
     *                             reported as empty TestDiagnosticLines
     * @return The List of TestDiagnosticLines from the input file
     */
    public static List<TestDiagnosticLine> readDiagnosticFile(File toRead, boolean omitEmptyDiagnostics) {
        return readDiagnostics(toRead, DIAGNOSTIC_FILE_CODEC, omitEmptyDiagnostics);
    }

    /**
     * Reads diagnostic lines line-by-line from the input Diagnostic files.
     * @param toRead A set of Diagnostic Files
     * @param omitEmptyDiagnostics Whether or not lines that do not contain any diagnostics should be
     *                             reported as empty TestDiagnosticLines
     * @return The List of TestDiagnosticLines from the input files
     */
    public static List<TestDiagnosticLine> readDiagnosticFileLines(Iterable<? extends File> toRead, boolean omitEmptyDiagnostics) {
        List<TestDiagnosticLine> lines = new ArrayList<>();
        for (File file : toRead) {
            lines.addAll(readDiagnosticFile(file, omitEmptyDiagnostics));
        }
        return lines;
    }

    /**
     * Reads diagnostics line-by-line from the input Diagnostic files.
     * @param toRead A set of Diagnostic Files
     * @param omitEmptyDiagnostics Whether or not lines that do not contain any diagnostics should be
     *                             reported as empty TestDiagnosticLines
     * @return The List of TestDiagnosticLines from the input files
     */
    public static List<TestDiagnostic> readDiagnosticFiles(Iterable<? extends File> toRead, boolean omitEmptyDiagnostics) {
        List<TestDiagnosticLine> lines = readDiagnosticFileLines(toRead, omitEmptyDiagnostics);

        List<TestDiagnostic> diagnostics = new ArrayList<TestDiagnostic>((int) (lines.size() + lines.size() * 0.1));
        for (TestDiagnosticLine line : lines) {
            diagnostics.addAll(line.getDiagnostics());
        }
        return diagnostics;
    }

    /**
     * Instances of DiagnosticCodec represent the various formats diagnostic strings can take
     */
    public interface DiagnosticCodec {
        public TestDiagnosticLine convertLine(long lineNumber, String line);
    }

    /**
     * Interprets a string that was written as a comment in a Java file
     */
    public static DiagnosticCodec JAVA_COMMENT_CODEC = new DiagnosticCodec() {
        @Override
        public TestDiagnosticLine convertLine(long lineNumber, String line) {
            return TestDiagnosticUtils.fromJavaSourceLine(line, lineNumber);
        }
    };

    /**
     * Interprets a string that was written as a line in a Diagnostic File
     */
    public static DiagnosticCodec DIAGNOSTIC_FILE_CODEC = new DiagnosticCodec() {
        @Override
        public TestDiagnosticLine convertLine(long lineNumber, String line) {
            return TestDiagnosticUtils.fromDiagnosticFileLine(line);
        }
    };

    public final File toRead;
    public final JavaFileObject toReadFileObject;
    public final DiagnosticCodec codec;

    private boolean initialized = false;
    private boolean closed = false;

    private LineNumberReader reader = null;

    public String nextLine = null;
    public int nextLineNumber = -1;

    public JavaDiagnosticReader(File toRead, DiagnosticCodec codec) {
        this.toRead = toRead;
        this.toReadFileObject = null;
        this.codec = codec;
    }

    public JavaDiagnosticReader(JavaFileObject toRead, DiagnosticCodec codec) {
        this.toRead = null;
        this.toReadFileObject = toRead;
        this.codec = codec;
    }

    private void init() throws IOException {
        if (!initialized && !closed) {
            initialized = true;

            Reader fileReader = (toRead != null) ? new FileReader(toRead) : toReadFileObject.openReader(true);
            reader = new LineNumberReader(fileReader);
            advance();
        }
    }

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
        throw new UnsupportedOperationException("Cannot remove elements using JavaDiagnosticFileReader.");
    }

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

            if (nextLine == null) {
                close();
            }

            return codec.convertLine(currentLineNumber, current);

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
