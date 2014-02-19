package java.io;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import checkers.javari.quals.*;

public class OutputStreamWriter extends Writer {
    protected OutputStreamWriter() {}

    public OutputStreamWriter(OutputStream out, String charsetName)
    throws UnsupportedEncodingException {
        throw new RuntimeException("skeleton method");
    }

    public OutputStreamWriter(OutputStream out) {
        throw new RuntimeException("skeleton method");
    }

    public OutputStreamWriter(OutputStream out, Charset cs) {
        throw new RuntimeException("skeleton method");
    }

    public OutputStreamWriter(OutputStream out, CharsetEncoder enc) {
        throw new RuntimeException("skeleton method");
    }

    public String getEncoding() {
        throw new RuntimeException("skeleton method");
    }

    void flushBuffer() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void write(int c) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void write(char @ReadOnly [] cbuf, int off, int len) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void write(String str, int off, int len) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void flush() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void close() throws IOException {
        throw new RuntimeException("skeleton method");
    }
}
