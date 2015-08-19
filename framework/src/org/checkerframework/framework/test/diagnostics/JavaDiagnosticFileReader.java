package org.checkerframework.framework.test.diagnostics;

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Reads an entire Java source file
 */
public class JavaDiagnosticFileReader {

    public static List<TestDiagnosticLine> readDiagnostics(File toRead, boolean omitEmptyDiagnostics) {
        List<TestDiagnosticLine> lines = new ArrayList<>();
        JavaDiagnosticFileReader reader = new JavaDiagnosticFileReader(toRead);
        while(reader.hasNext()) {
            TestDiagnosticLine line = reader.next();
            if (!omitEmptyDiagnostics || line.hasDiagnostics()) {
                lines.add(line);
            }
        }
        reader.close();

        return lines;
    }



    public static List<TestDiagnosticLine> readDiagnosticLines(Iterable<File> toRead, boolean omitEmptyDiagnostics) {
        List<TestDiagnosticLine> lines = new ArrayList<>();
        for (File file : toRead) {
            lines.addAll(readDiagnostics(file, omitEmptyDiagnostics));
        }
        return lines;
    }

    public static List<TestDiagnostic> readDiagnostics(Iterable<File> toRead, boolean omitEmptyDiagnostics) {
        List<TestDiagnosticLine> lines = readDiagnosticLines(toRead, omitEmptyDiagnostics);

        List<TestDiagnostic> diagnostics = new ArrayList<TestDiagnostic>((int) (lines.size() + lines.size() * 0.1));
        for (TestDiagnosticLine line : lines) {
            diagnostics.addAll(line.getDiagnostics());
        }
        return diagnostics;
    }

    public static List<TestDiagnosticLine> readDiagnosticsJfo(JavaFileObject toRead, boolean omitEmptyDiagnostics) {
        List<TestDiagnosticLine> lines = new ArrayList<>();
        JavaDiagnosticFileReader reader = new JavaDiagnosticFileReader(toRead);
        while(reader.hasNext()) {
            TestDiagnosticLine line = reader.next();
            if (!omitEmptyDiagnostics || line.hasDiagnostics()) {
                lines.add(line);
            }
        }
        reader.close();

        return lines;
    }

    public static List<TestDiagnosticLine> readExpectedDiagnosticLinesJfo(Iterable<? extends JavaFileObject> toRead, boolean omitEmptyDiagnostics) {
        List<TestDiagnosticLine> lines = new ArrayList<>();
        for (JavaFileObject file : toRead) {
            lines.addAll(readDiagnosticsJfo(file, omitEmptyDiagnostics));
        }
        return lines;
    }

    public static List<TestDiagnostic> readExpectedDiagnosticsJfo(Iterable<? extends JavaFileObject> toRead, boolean omitEmptyDiagnostics) {
        List<TestDiagnosticLine> lines = readExpectedDiagnosticLinesJfo(toRead, omitEmptyDiagnostics);

        List<TestDiagnostic> diagnostics = new ArrayList<TestDiagnostic>((int) (lines.size() + lines.size() * 0.1));
        for (TestDiagnosticLine line : lines) {
            diagnostics.addAll(line.getDiagnostics());
        }
        return diagnostics;
    }

    public static List<TestDiagnosticLine> readDiagnosticFileLines(Iterable<? extends JavaFileObject> toRead, boolean omitEmptyDiagnostics) {
        List<TestDiagnosticLine> lines = new ArrayList<>();
        for (JavaFileObject file : toRead) {
            lines.addAll(readDiagnosticsJfo(file, omitEmptyDiagnostics));
        }
        return lines;
    }

    public static List<TestDiagnostic> readDiagnosticFiles(Iterable<? extends JavaFileObject> toRead, boolean omitEmptyDiagnostics) {
        List<TestDiagnosticLine> lines = readExpectedDiagnosticLinesJfo(toRead, omitEmptyDiagnostics);

        List<TestDiagnostic> diagnostics = new ArrayList<TestDiagnostic>((int) (lines.size() + lines.size() * 0.1));
        for (TestDiagnosticLine line : lines) {
            diagnostics.addAll(line.getDiagnostics());
        }
        return diagnostics;
    }

    public final File toRead;
    public final JavaFileObject toReadFileObject;

    private boolean initialized = false;
    private boolean closed = false;

    private LineNumberReader reader = null;

    public String nextLine = null;
    public int nextLineNumber = -1;

    public JavaDiagnosticFileReader(File toRead) {
        this.toRead = toRead;
        this.toReadFileObject = null;
    }

    public JavaDiagnosticFileReader(JavaFileObject toRead) {
        this.toRead = null;
        this.toReadFileObject = toRead;
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

            return TestDiagnosticUtils.fromJavaSourceLine(current, currentLineNumber);

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
