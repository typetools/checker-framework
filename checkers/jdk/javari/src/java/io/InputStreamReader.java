package java.io;
import checkers.javari.quals.*;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class InputStreamReader extends Reader {
    protected InputStreamReader() {}

    public InputStreamReader(InputStream in) {
        throw new RuntimeException("skeleton method");
    }

    public InputStreamReader(InputStream in, String charsetName)
        throws UnsupportedEncodingException {
        throw new RuntimeException("skeleton method");
    }

    public InputStreamReader(InputStream in, Charset cs) {
        throw new RuntimeException("skeleton method");
    }

    public InputStreamReader( InputStream in,  CharsetDecoder dec) {
        throw new RuntimeException("skeleton method");
    }

    public String getEncoding() {
        throw new RuntimeException("skeleton method");
    }

    public int read() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public int read(char cbuf[], int offset, int length) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public boolean ready() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void close() throws IOException {
        throw new RuntimeException("skeleton method");
    }
}
