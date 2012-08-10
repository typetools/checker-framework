package java.io;
import checkers.javari.quals.*;

public class StringReader extends Reader {
    public StringReader(String s) {
        throw new RuntimeException("skeleton method");
    }

    public int read() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public int read(char cbuf[], int off, int len) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public long skip(long ns) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public boolean ready(@ReadOnly StringReader this) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public boolean markSupported(@ReadOnly StringReader this) {
        throw new RuntimeException("skeleton method"); 
    }

    public void mark(int readAheadLimit) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void reset() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void close() {
        throw new RuntimeException("skeleton method");
    }
}
