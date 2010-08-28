package java.io;
import checkers.javari.quals.*;

public class BufferedWriter extends Writer {

    private Writer out;

    private char cb[];
    private int nChars, nextChar;

    private static int defaultCharBufferSize = 8192;

    private String lineSeparator;

    public BufferedWriter(Writer out) {
        throw new RuntimeException("skeleton method");
    }

    public BufferedWriter(Writer out, int sz) {
        throw new RuntimeException("skeleton method");
    }

    public void write(int c) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void write(char cbuf @ReadOnly [], int off, int len) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void write(String s, int off, int len) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void newLine() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void flush() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void close() throws IOException {
        throw new RuntimeException("skeleton method");
    }
}
