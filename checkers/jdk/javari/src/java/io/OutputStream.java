package java.io;

import checkers.javari.quals.*;

public abstract class OutputStream implements Closeable, Flushable {
    public abstract void write(int b) throws IOException;

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
