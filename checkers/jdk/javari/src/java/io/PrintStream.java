package java.io;

import java.util.Formatter;
import java.util.Locale;

import checkers.javari.quals.*;

public class PrintStream extends FilterOutputStream
    implements Appendable, Closeable
{

    public PrintStream(OutputStream out) { super(null); }

    public PrintStream(OutputStream out, boolean autoFlush) { super(null); }

    public PrintStream(OutputStream out, boolean autoFlush, String encoding)
        throws UnsupportedEncodingException { super(null); }

    public PrintStream(String fileName) throws FileNotFoundException {
	super(null);
        throw new RuntimeException("skeleton method");
    }

    public PrintStream(String fileName, String csn)
    throws FileNotFoundException, UnsupportedEncodingException {
	super(null);
        throw new RuntimeException("skeleton method");
    }

    public PrintStream(File file) throws FileNotFoundException {
	super(null);
        throw new RuntimeException("skeleton method");
    }

    public PrintStream(File file, String csn)
    throws FileNotFoundException, UnsupportedEncodingException {
	super(null);
        throw new RuntimeException("skeleton method");
    }

    public void flush() {
        throw new RuntimeException("skeleton method");
    }

    public void close() {
        throw new RuntimeException("skeleton method");
    }

    public boolean checkError() {
        throw new RuntimeException("skeleton method");
    }

    protected void setError() {
        throw new RuntimeException("skeleton method");
    }

    protected void clearError() {
        throw new RuntimeException("skeleton method");
    }

    public void write(int b) {
        throw new RuntimeException("skeleton method");
    }

    public void write(byte @ReadOnly [] buf, int off, int len) {
        throw new RuntimeException("skeleton method");
    }

    public void print(boolean b) {
        throw new RuntimeException("skeleton method");
    }

    public void print(char c) {
        throw new RuntimeException("skeleton method");
    }

    public void print(int i) {
        throw new RuntimeException("skeleton method");
    }

    public void print(long l) {
        throw new RuntimeException("skeleton method");
    }

    public void print(float f) {
        throw new RuntimeException("skeleton method");
    }

    public void print(double d) {
        throw new RuntimeException("skeleton method");
    }

    public void print(char @ReadOnly [] s) {
        throw new RuntimeException("skeleton method");
    }

    public void print(String s) {
        throw new RuntimeException("skeleton method");
    }

    public void print(@ReadOnly Object obj) {
        throw new RuntimeException("skeleton method");
    }

    public void println() {
        throw new RuntimeException("skeleton method");
    }

    public void println(boolean x) {
        throw new RuntimeException("skeleton method");
    }

    public void println(char x) {
        throw new RuntimeException("skeleton method");
    }

    public void println(int x) {
        throw new RuntimeException("skeleton method");
    }

    public void println(long x) {
        throw new RuntimeException("skeleton method");
    }

    public void println(float x) {
        throw new RuntimeException("skeleton method");
    }

    public void println(double x) {
        throw new RuntimeException("skeleton method");
    }

    public void println(char @ReadOnly [] x) {
        throw new RuntimeException("skeleton method");
    }

    public void println(String x) {
        throw new RuntimeException("skeleton method");
    }

    public void println(@ReadOnly Object x) {
        throw new RuntimeException("skeleton method");
    }

    public PrintStream printf(String format, @ReadOnly Object @ReadOnly ... args) {
        throw new RuntimeException("skeleton method");
    }

    public PrintStream printf(Locale l, String format, @ReadOnly Object @ReadOnly ... args) {
        throw new RuntimeException("skeleton method");
    }

    public PrintStream format(String format, @ReadOnly Object @ReadOnly ... args) {
        throw new RuntimeException("skeleton method");
    }

    public PrintStream format(Locale l, String format, @ReadOnly Object @ReadOnly ... args) {
        throw new RuntimeException("skeleton method");
    }

    public PrintStream append(@ReadOnly CharSequence csq) {
        throw new RuntimeException("skeleton method");
    }

    public PrintStream append(@ReadOnly CharSequence csq, int start, int end) {
        throw new RuntimeException("skeleton method");
    }

    public PrintStream append(char c) {
        throw new RuntimeException("skeleton method");
    }
}
