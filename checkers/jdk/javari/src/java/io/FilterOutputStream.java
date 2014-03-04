package java.io;

import checkers.javari.quals.*;

public class FilterOutputStream extends OutputStream {
    protected OutputStream out;

    protected FilterOutputStream() {}
    public FilterOutputStream(OutputStream out) { }

    public void write(int b) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void write(byte @ReadOnly [] b) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void write(byte @ReadOnly [] b, int off, int len) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void flush() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void close() throws IOException {
        throw new RuntimeException("skeleton method");
    }
}
