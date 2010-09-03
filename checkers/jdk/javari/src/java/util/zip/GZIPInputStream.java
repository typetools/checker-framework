package java.util.zip;
import checkers.javari.quals.*;

import java.io.InputStream;
import java.io.IOException;

public class GZIPInputStream extends InflaterInputStream {
    public GZIPInputStream(InputStream in, int size) throws IOException {
        super(in);
        throw new RuntimeException("skeleton method");
    }

    public GZIPInputStream(InputStream in) throws IOException {
        super(in);
        throw new RuntimeException("skeleton method");
    }

    public int read(byte[] buf, int off, int len) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void close() throws IOException {
        throw new RuntimeException("skeleton method");
    }
}
