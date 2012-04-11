package java.io;
import checkers.javari.quals.*;

public abstract class InputStream implements Closeable {
    public abstract int read() throws IOException;

    public int read(byte b[]) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public int read(byte b[], int off, int len) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public long skip(long n) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public int available() @ReadOnly throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void close() throws IOException {}

    public synchronized void mark(int readlimit) {}

    public synchronized void reset() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public boolean markSupported() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }
}
